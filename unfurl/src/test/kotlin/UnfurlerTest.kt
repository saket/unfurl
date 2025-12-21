package me.saket.unfurl

import app.cash.turbine.Turbine
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isLessThan
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import me.saket.bytesize.decimalBytes
import me.saket.bytesize.kilobytes
import me.saket.bytesize.megabits
import me.saket.unfurl.extension.HtmlMetadataUnfurlerExtension
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.RecordedRequest
import mockwebserver3.junit4.MockWebServerRule
import okhttp3.Call
import okhttp3.EventListener
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okio.Buffer
import okio.GzipSink
import okio.buffer
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTimedValue

@RunWith(TestParameterInjector::class)
class UnfurlerTest {
  @get:Rule val timeout = Timeout(10, TimeUnit.SECONDS)
  @get:Rule val serverRule = MockWebServerRule()
  private val server get() = serverRule.server

  @Test fun `parse HTML correctly`(@TestParameter input: HtmlTestInput) = runTest {
    server.enqueue(
      MockResponse.Builder()
        .setHeader("Content-Type", "text/html; charset=UTF-8")
        .body(readResourceFile(input.htmlFileName))
        .build()
    )

    val localUrl = server.url(input.url.removePrefix("https:/"))
    val result = Unfurler().unfurl(localUrl)
    assertThat(result).isEqualTo(input.expected(localUrl))
    assertThat(server.requestCount).isEqualTo(1)
  }

  @Test fun `websites that deny requests without a recognizable user-agent`() = runTest {
    val result = Unfurler().unfurl("https://www.getproactiv.ca/pdp?productcode=842944100695")
    assertThat(result).isEqualTo(
      UnfurlResult(
        url = "https://www.getproactiv.ca/proactiv-solution-repairing-treatment/p/842944100695?productcode=842944100695".toHttpUrl(),
        title = "Proactiv Solution® Repairing Treatment | Proactiv® Products",
        description = "Our Repairing Treatment is a leave-on treatment formulated with prescription-grade benzoyl peroxide designed to penetrate pores to kill acne-causing bacteria.",
        favicon = "https://www.getproactiv.ca/favicon.ico".toHttpUrl(),
        thumbnail = "https://cdn-tp3.mozu.com/30113-50629/cms/50629/files/f050a010-0420-4a53-b898-d4c08db77eb9".toHttpUrl(),
      )
    )
  }

  @Ignore("Nitter intances get rate limited very frequently. Also see: https://github.com/zedeus/nitter/wiki/Instances")
  @Test fun `websites that deny requests without content type and language headers`() = runTest {
    val result = Unfurler().unfurl("https://nitter.privacydev.net/saketme/status/1716330453311877183")
    assertThat(result).isEqualTo(
      UnfurlResult(
        url = "https://nitter.privacydev.net/saketme/status/1716330453311877183".toHttpUrl(),
        title = "saket@androiddev.social (@saketme)",
        description = "When the sole developer of a project starts using \"we\" instead of \"I\" in their code comments.",
        favicon = "https://nitter.privacydev.net/apple-touch-icon.png".toHttpUrl(),
        thumbnail = "https://nitter.privacydev.net/pic/media%2FF9GhXLmXYAAXIcb.png".toHttpUrl(),
      )
    )
  }

  @Test fun `try out all user agents for websites that block some agents (real)`() = runTest {
    val unfurler = Unfurler()

    // kraken.com frequently returns an HTTP 403 for Chrome's user agent, but allows Slack and WhatsApp.
    with(unfurler.unfurl("https://kraken.com")) {
      assertThat(this?.title.orEmpty()).contains("Kraken", ignoreCase = true)
    }

    // bestbuy.com throttles requests with Slack's user agent by 8-9s.
    with(unfurler.unfurl("https://bestbuy.com")) {
      assertThat(this?.title.orEmpty()).contains("Best Buy", ignoreCase = true)
    }

    // aa.com times out for Slack's user agent.
    with(unfurler.unfurl("https://aa.com")) {
      assertThat(this?.title.orEmpty()).contains("American Airlines", ignoreCase = true)
    }

    // notion.so returns an empty HTML for Slack's user agent and HTTP 404 for WhatsApp's.
    with(unfurler.unfurl("https://www.notion.so/Test-5dd9c63227584bb494966fba4f4e002d")) {
      assertThat(this?.title.orEmpty()).contains("Notion", ignoreCase = true)
    }
  }

  @Test fun `try out all user agents for websites that block some agents (fake)`() = runTest {
    val userAgents = listOf(
      "UserAgent 403",
      "UserAgent Timeout",
      "UserAgent Empty Html",
      "UserAgent 200",
    )

    val serverDispatcher = object : Dispatcher() {
      val requestedAgents = Turbine<String>(name = "requested agents")
      val responses = MutableStateFlow(mapOf<String, MockResponse>())

      override fun dispatch(request: RecordedRequest): MockResponse {
        val userAgent = request.headers["User-Agent"]!!
        requestedAgents.add(userAgent)
        return runBlocking {
          responses.mapNotNull { it[userAgent] }.first()
        }
      }
    }
    server.dispatcher = serverDispatcher

    val responseBodyTracker = ResponseBodyTracker()
    val unfurler = Unfurler(
      extensions = listOf(HtmlMetadataUnfurlerExtension(userAgents)),
      httpClient = Unfurler.defaultOkHttpClient()
        .newBuilder()
        .eventListener(responseBodyTracker)
        .build(),
    )

    flow {
      emit(unfurler.unfurl(server.url("/")))
    }.test {
      expectNoEvents()

      // The first user agent should be sent immediately, without any delay.
      assertThat(serverDispatcher.requestedAgents.awaitItem()).isEqualTo("UserAgent 403")
      serverDispatcher.responses.update {
        val response403 = MockResponse.Builder()
          .code(403)
          .setHeader("Content-Type", "text/html")
          .body("<html><head><title>Access Denied</title></head></html>")
          .build()
        it + ("UserAgent 403" to response403)
      }
      expectNoEvents()

      // Wait for "UserAgent Timeout" to be sent.
      serverDispatcher.requestedAgents.expectNoEvents()
      Thread.sleep(HtmlMetadataUnfurlerExtension.DelayForFallbackUserAgents.inWholeMilliseconds)
      assertThat(serverDispatcher.requestedAgents.awaitItem()).isEqualTo("UserAgent Timeout")

      // "UserAgent Timeout" times out, so no HTML is ever downloaded.
      serverDispatcher.responses.update {
        val responseTimeout = MockResponse.Builder()
          .headersDelay(Long.MAX_VALUE, TimeUnit.MILLISECONDS)
          .build()
        it + ("UserAgent Timeout" to responseTimeout)
      }
      expectNoEvents()

      // Wait for "UserAgent Empty Html".
      serverDispatcher.requestedAgents.expectNoEvents()
      Thread.sleep(HtmlMetadataUnfurlerExtension.DelayForFallbackUserAgents.inWholeMilliseconds)
      assertThat(serverDispatcher.requestedAgents.awaitItem()).isEqualTo("UserAgent Empty Html")

      // "UserAgent Empty Html" receives an empty HTML that doesn't contain any social metadata.
      serverDispatcher.responses.update {
        val responseEmptyHtml = MockResponse.Builder()
          .setHeader("Content-Type", "text/html")
          .body("<html><head><title></title></head></html>")
          .build()
        it + ("UserAgent Empty Html" to responseEmptyHtml)
      }
      expectNoEvents()

      // Wait for "UserAgent 200".
      serverDispatcher.requestedAgents.expectNoEvents()
      Thread.sleep(HtmlMetadataUnfurlerExtension.DelayForFallbackUserAgents.inWholeMilliseconds)
      assertThat(serverDispatcher.requestedAgents.awaitItem()).isEqualTo("UserAgent 200")

      // "UserAgent 200" succeeds. The HTML is downloaded and parsed.
      serverDispatcher.responses.update {
        val response200 = MockResponse.Builder()
          .setHeader("Content-Type", "text/html")
          .body(readResourceFile("html_source_saket.me.html"))
          .build()
        it + ("UserAgent 200" to response200)
      }
      assertThat(awaitItem()?.title).isEqualTo("Great teams merge fast")

      // Verify that all response bodies were closed.
      assertThat(responseBodyTracker.openBodies).isEmpty()

      awaitComplete()
    }
  }

  @Test fun `when the first user agent succeeds, do not use any remaining agents`() = runTest {
    val userAgents = listOf("UserAgent 1", "UserAgent 2", "UserAgent 3")
    val requestedUserAgents = mutableListOf<String>()

    server.dispatcher = object : Dispatcher() {
      override fun dispatch(request: RecordedRequest): MockResponse {
        val userAgent = request.headers["User-Agent"]!!
        requestedUserAgents.add(userAgent)

        return MockResponse.Builder()
          .setHeader("Content-Type", "text/html")
          .body(readResourceFile("html_source_saket.me.html"))
          .build()
      }
    }

    val unfurler = Unfurler(
      extensions = listOf(HtmlMetadataUnfurlerExtension(userAgents))
    )
    val result = unfurler.unfurl(server.url("/"))
    assertThat(result?.title).isEqualTo("Great teams merge fast")

    // The user agents after the first one are delayed. If the first one
    // returns within the delay, the remaining ones should not even fire.
    assertThat(requestedUserAgents).containsExactly("UserAgent 1")
  }

  @Test fun `when the second user agent succeeds, response bodies of remaining requests are not streamed`() = runTest {
    val userAgents = listOf(
      "UserAgent 403",
      "UserAgent Timeout",
      "UserAgent 200",
    )

    server.dispatcher = object : Dispatcher() {
      override fun dispatch(request: RecordedRequest): MockResponse {
        return when (request.headers["User-Agent"]) {
          "UserAgent 403" -> {
            MockResponse.Builder()
              .code(403)
              .setHeader("Content-Type", "text/html")
              .body("<html><head><title>Forbidden</title></head></html>")
              .build()
          }
          "UserAgent Timeout" -> {
            MockResponse.Builder()
              .headersDelay(Long.MAX_VALUE, TimeUnit.MILLISECONDS)
              .build()
          }
          "UserAgent 200" -> {
            MockResponse.Builder()
              .setHeader("Content-Type", "text/html")
              .body(readResourceFile("html_source_saket.me.html"))
              .headersDelay(500, TimeUnit.MILLISECONDS)
              .build()
          }
          else -> error("Unknown user agent")
        }
      }
    }

    val streamedUserAgents = mutableListOf<String>()
    val eventListener = object : EventListener() {
      override fun responseBodyStart(call: Call) {
        streamedUserAgents.add(call.request().header("User-Agent")!!)
      }
    }

    val unfurler = Unfurler(
      httpClient = Unfurler.defaultOkHttpClient()
        .newBuilder()
        .eventListener(eventListener)
        .build(),
      extensions = listOf(HtmlMetadataUnfurlerExtension(userAgents)),
    )

    val result = unfurler.unfurl(server.url("/"))
    assertThat(result?.title).isEqualTo("Great teams merge fast")
    assertThat(streamedUserAgents).containsExactly("UserAgent 403", "UserAgent 200")
  }

  @Test fun `follow redirects`() = runTest {
    server.enqueue(
      MockResponse.Builder()
        .code(303)
        .setHeader("Location", "https://www.youtube.com/watch?v=o-YBDTqX_ZU&feature=youtu.be")
        .build()
    )

    val result = Unfurler().unfurl(server.url("/youtu.be/o-YBDTqX_ZU"))
    assertThat(result).isNotNull()
    assertThat(server.takeRequest()).isNotNull()
  }

  @Test fun `cache unfurled urls`() = runTest {
    server.enqueue(
      MockResponse.Builder()
        .setHeader("Content-Type", "text/html; charset=UTF-8")
        .body(readResourceFile("html_source_saket.me.html"))
        .build()
    )

    val unfurler = Unfurler()
    val result = unfurler.unfurl(server.url("foo"))
    assertThat(result?.title).isEqualTo("Great teams merge fast")
    val requestCountAfterFirstUnfurl = server.requestCount

    repeat(3) {
      val result = unfurler.unfurl(server.url("foo"))
      assertThat(result?.title).isEqualTo("Great teams merge fast")
      assertThat(server.requestCount).isEqualTo(requestCountAfterFirstUnfurl)
    }
  }

  @Test fun `cancel the network call when unfurling is cancelled`() = runTest {
    val neverCompleteThisRequest = CountDownLatch(1)
    server.dispatcher = object : Dispatcher() {
      override fun dispatch(request: RecordedRequest): MockResponse {
        // This uses a CountDownLatch instead of MockWebServer's built-in
        // delay/throttle APIs because they use Thread.sleep(), which blocks
        // the thread and prevents proper coroutine cancellation.
        neverCompleteThisRequest.await()
        return MockResponse.Builder().build()
      }
    }
    val httpEventListener = object : EventListener() {
      val startedCalls = Turbine<Call>(name = "started calls")
      val canceledCalls = Turbine<Call>(name = "canceled calls")
      override fun canceled(call: Call) = canceledCalls.add(call)
      override fun callStart(call: Call) = startedCalls.add(call)
    }
    val unfurler = Unfurler(
      httpClient = Unfurler.defaultOkHttpClient()
        .newBuilder()
        .eventListener(httpEventListener)
        .build(),
    )

    withContext(Dispatchers.NoDelaySkipping) {
      withTimeoutOrNull(1000.milliseconds) {
        unfurler.unfurl(server.url("ignored"))
      }
    }

    httpEventListener.startedCalls.awaitItem()
    httpEventListener.canceledCalls.awaitItem()

    // Shut down the web server.
    neverCompleteThisRequest.countDown()
  }

  @Test fun `avoid downloading entire web pages by streaming them instead`() = runTest {
    // Simulate a download of a large web page on a slow connection.
    // At 2Mbps, downloading the full 1.5MB html file would take ~6 seconds.
    server.enqueue(
      MockResponse.Builder()
        .setHeader("Content-Type", "text/html; charset=UTF-8")
        .setHeader("Content-Encoding", "gzip")
        .body(readResourceFile("html_source_nytimes_best_movies.html").gzipped())
        .throttleBody(2.megabits.inWholeBytes, 1, TimeUnit.SECONDS)
        .build()
    )
    var totalBytesDownloaded = 0.decimalBytes

    val (result, duration) = measureTimedValue {
      val unfurler = Unfurler(
        httpClient = Unfurler.defaultOkHttpClient()
          .newBuilder()
          .eventListener(object : EventListener() {
            override fun responseBodyEnd(call: Call, byteCount: Long) {
              totalBytesDownloaded += byteCount.decimalBytes
            }
          })
          .build(),
      )
      unfurler.unfurl(server.url("/"))
    }
    assertThat(result?.title).isEqualTo("The 100 Best Movies of the 21st Century")
    assertThat(totalBytesDownloaded).isLessThan(40.kilobytes)
    assertThat(duration).isLessThan(0.3.seconds)
  }

  @Test fun `response bodies are closed after failed unfurl`() = runTest {
    server.dispatcher = object : Dispatcher() {
      override fun dispatch(request: RecordedRequest) =
        MockResponse.Builder()
          .code(403)
          .setHeader("Content-Type", "text/html")
          .body("<html><head><title>Forbidden</title></head></html>")
          .build()
    }

    val responseBodyTracker = ResponseBodyTracker()
    val unfurler = Unfurler(
      httpClient = Unfurler.defaultOkHttpClient()
        .newBuilder()
        .eventListener(responseBodyTracker)
        .build(),
      extensions = listOf(HtmlMetadataUnfurlerExtension(listOf("agent1", "agent2", "agent3")))
    )

    val result = unfurler.unfurl(server.url("/"))
    assertThat(result).isNull()
    assertThat(responseBodyTracker.openBodies).isEmpty()
  }

  @Test fun `html without head element`() = runTest {
    server.dispatcher = object : Dispatcher() {
      override fun dispatch(request: RecordedRequest) =
        MockResponse.Builder()
          .setHeader("Content-Type", "text/html; charset=UTF-8")
          .body("<html><body><p>No head element here</p></body></html>")
          .build()
    }

    val url = server.url("/")

    val unfurler = Unfurler(
      logger = UnfurlLogger.println(),
    )
    assertThat(unfurler.unfurl(url)).isEqualTo(
      UnfurlResult(
        url = url,
        title = null,
        description = null,
        thumbnail = null,
        favicon = "http://localhost/favicon.ico".toHttpUrl(),
      )
    )
  }

  @Test fun `http 404`() = runTest {
    server.dispatcher = object : Dispatcher() {
      override fun dispatch(request: RecordedRequest) =
        MockResponse.Builder()
          .code(404)
          .build()
    }

    val unfurler = Unfurler(
      logger = UnfurlLogger.println(),
    )
    assertThat(unfurler.unfurl(server.url("/"))).isNull()
  }

  private fun readResourceFile(fileName: String): String {
    val url = Thread.currentThread().contextClassLoader.getResource(fileName)!!
    return File(url.path).readText()
  }

  @Suppress("EnumEntryName", "unused")
  enum class HtmlTestInput(
    val url: String,
    val htmlFileName: String,
    val expected: (localUrl: HttpUrl) -> UnfurlResult,
  ) {
    Saket_me( // Uses both OGP and twitter meta tags.
      url = "https://saket.me/great-teams-merge-fast/",
      htmlFileName = "html_source_saket.me.html",
      expected = { localUrl ->
        UnfurlResult(
          url = localUrl,
          title = "Great teams merge fast",
          description = "Observations from watching my team at Square produce stellar work while moving fast and not breaking things.",
          thumbnail = "https://saket.me/wp-content/uploads/2021/02/great_teams_merge_fast_cover.jpg".toHttpUrl(),
          favicon = "https://saket.me/wp-content/uploads/2022/03/cropped-saket-photo-180x180.jpg".toHttpUrl(),
        )
      }
    ),
    Instagram_com(  // Does not use most twitter meta tags.
      url = "https://about.instagram.com/",
      htmlFileName = "html_source_instagram.com.html",
      expected = { localUrl ->
        UnfurlResult(
          url = localUrl,
          title = "About Instagram's Official Site",
          description = "We strive to bring people together in a safe and supportive community. We believe expression is the greatest connector. Make the most of your Instagram experience!",
          thumbnail = "https://scontent-ort2-2.xx.fbcdn.net/v/t39.2365-6/75883158_790065824784383_3063578611500974080_n.jpg?_nc_cat=109&ccb=1-7&_nc_sid=ad8a9d&_nc_ohc=AIIVl_p_K0gAX9iad2X&_nc_ht=scontent-ort2-2.xx&oh=00_AT-0skyUQDSPTdFriyws79pzJ1z1Z9geycC9kp-dq4Mw3A&oe=62F7BDFE".toHttpUrl(),
          favicon = "https://static.xx.fbcdn.net/rsrc.php/v3/yw/r/HTE9u6HBvgx.png".toHttpUrl(),
        )
      }
    ),
    Gitless_com(  // Does not use any social tags.
      url = "https://gitless.com",
      htmlFileName = "html_source_gitless.com.html",
      expected = { localUrl ->
        UnfurlResult(
          url = localUrl,
          title = "Gitless",
          description = "Gitless: a simple version control system built on top of Git",
          thumbnail = null,
          favicon = localUrl.newBuilder()
            .encodedPath("/favicon.ico")
            .build()
        )
      }
    )
  }
}

/** Because runTest() skips delays by default. */
@Suppress("UnusedReceiverParameter")
@OptIn(ExperimentalCoroutinesApi::class)
val Dispatchers.NoDelaySkipping: CoroutineDispatcher
  get() = Dispatchers.Default.limitedParallelism(1)

private class ResponseBodyTracker : EventListener() {
  var openBodies = mutableListOf<Call>()

  override fun responseBodyStart(call: Call) {
    openBodies.add(call)
  }

  override fun responseBodyEnd(call: Call, byteCount: Long) {
    openBodies.remove(call)
  }
}

private fun String.gzipped(): Buffer {
  return Buffer().apply {
    GzipSink(this).buffer().use { it.writeUtf8(this@gzipped) }
  }
}
