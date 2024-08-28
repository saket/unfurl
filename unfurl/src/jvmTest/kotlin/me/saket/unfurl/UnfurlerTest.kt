package me.saket.unfurl

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Call
import okhttp3.EventListener
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds

@RunWith(TestParameterInjector::class)
class UnfurlerTest {
  @get:Rule val server = MockWebServer()
  @get:Rule val timeout = Timeout(5, TimeUnit.SECONDS)

  @Test fun `parse HTML correctly`(@TestParameter input: HtmlTestInput) = runTest {
    server.enqueue(
      MockResponse()
        .setHeader("Content-Type", "text/html; charset=UTF-8")
        .setBody(readResourceFile(input.htmlFileName))
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

  @Test fun `follow redirects`() = runTest {
    server.enqueue(
      MockResponse()
        .setResponseCode(303)
        .setHeader("Location", "https://www.youtube.com/watch?v=o-YBDTqX_ZU&feature=youtu.be")
    )

    val result = Unfurler().unfurl(server.url("/youtu.be/o-YBDTqX_ZU"))
    assertThat(result).isNotNull()
    assertThat(server.takeRequest()).isNotNull()
  }

  @Test fun `cache unfurled urls`() = runTest {
    server.enqueue(
      MockResponse()
        .setHeader("Content-Type", "text/html; charset=UTF-8")
        .setBody(readResourceFile("html_source_saket.me.html"))
    )

    val unfurler = Unfurler()
    repeat(3) {
      val result = unfurler.unfurl(server.url("foo"))
      assertThat(result?.title).isEqualTo("Great teams merge fast")
    }
    assertThat(server.requestCount).isEqualTo(1)
  }

  @Test fun `cancel the network call when unfurling is cancelled`() = runTest {
    server.enqueue(
      MockResponse()
        .setBodyDelay(Long.MAX_VALUE, TimeUnit.SECONDS)
    )

    val httpEventListener = object : EventListener() {
      var requestCanceled = false
      override fun canceled(call: Call) {
        requestCanceled = true
      }
    }
    val unfurler = Unfurler(
      httpClient = Unfurler.defaultOkHttpClient()
        .newBuilder()
        .eventListener(httpEventListener)
        .build(),
    )

    withTimeoutOrNull(100.milliseconds) {
      unfurler.unfurl(server.url("ignored"))
    }
    assertThat(httpEventListener.requestCanceled).isTrue()
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
