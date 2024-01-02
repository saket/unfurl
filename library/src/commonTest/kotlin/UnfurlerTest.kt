package me.saket.unfurl

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import io.ktor.utils.io.core.use
import me.saket.unfurl.internal.toUrl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class UnfurlerTest {
  private val unfurler = Unfurler()

  @Test
  fun `parse HTML correctly`() {
    parameterizedTest(enumValues<HtmlTestInput>()) { input ->
      val mockEngine = MockEngine.invoke {
        respond(
          content = readResourceFile(input.htmlFileName),
          status = HttpStatusCode.OK,
          headers = headersOf(HttpHeaders.ContentType, "text/html; charset=UTF-8")
        )
      }
      val server = HttpClient(mockEngine)
      server.use {
        val localUrl = input.url.toUrl()
        val result = unfurler.unfurl(localUrl)
        assertEquals(result, input.expected("".toUrl())?.copy(url = localUrl))
        // TODO: Clear server request queue
      }
    }
  }

  @Test
  fun `websites that deny requests without a recognizable user-agent`() {
    val result = unfurler.unfurl("https://www.getproactiv.ca/pdp?productcode=842944100695")
    assertEquals(
      result, UnfurlResult(
        url = "https://www.getproactiv.ca/pdp?productcode=842944100695".toUrl(),
        title = "Proactiv Solution® Repairing Treatment | Proactiv® Products",
        description = "Our Repairing Treatment is a leave-on treatment formulated with prescription-grade benzoyl peroxide designed to penetrate pores to kill acne-causing bacteria.",
        favicon = "https://www.getproactiv.ca/favicon.ico".toUrl(),
        thumbnail = "https://cdn-tp3.mozu.com/30113-50629/cms/50629/files/f050a010-0420-4a53-b898-d4c08db77eb9".toUrl(),
      )
    )
  }

  @Test
  fun `follow redirects`() {
    val mockEngine = MockEngine.invoke {
      respond(
        content = "",
        status = HttpStatusCode.SeeOther,
        headers = headersOf(HttpHeaders.Location, "https://www.youtube.com/watch?v=o-YBDTqX_ZU&feature=youtu.be")
      )
    }
    val server = HttpClient(mockEngine)
    server.use {
      val result = unfurler.unfurl("/youtu.be/o-YBDTqX_ZU".toUrl())
      assertNotNull(result)
      // TODO: Clear server request queue
    }
  }
}

@Suppress("EnumEntryName", "unused")
enum class HtmlTestInput(
  val url: String,
  val htmlFileName: String,
  val expected: (serverUrl: Url) -> UnfurlResult?
) {
  Saket_me( // Uses both OGP and twitter meta tags.
    url = "/saket.me/great-teams-merge-fast/",
    htmlFileName = "html_source_saket.me.html",
    expected = {
      UnfurlResult(
        url = "https://saket.me/great-teams-merge-fast/".toUrl(),
        title = "Great teams merge fast",
        description = "Observations from watching my team at Square produce stellar work while moving fast and not breaking things.",
        thumbnail = "https://saket.me/wp-content/uploads/2021/02/great_teams_merge_fast_cover.jpg".toUrl(),
        favicon = "https://saket.me/wp-content/uploads/2022/03/cropped-saket-photo-180x180.jpg".toUrl(),
      )
    }
  ),
  Instagram_com(  // Does not use most twitter meta tags.
    url = "/about.instagram.com",
    htmlFileName = "html_source_instagram.com.html",
    expected = {
      UnfurlResult(
        url = "https://about.instagram.com/".toUrl(),
        title = "About Instagram's Official Site",
        description = "We strive to bring people together in a safe and supportive community. We believe expression is the greatest connector. Make the most of your Instagram experience!",
        thumbnail = "https://scontent-ort2-2.xx.fbcdn.net/v/t39.2365-6/75883158_790065824784383_3063578611500974080_n.jpg?_nc_cat=109&ccb=1-7&_nc_sid=ad8a9d&_nc_ohc=AIIVl_p_K0gAX9iad2X&_nc_ht=scontent-ort2-2.xx&oh=00_AT-0skyUQDSPTdFriyws79pzJ1z1Z9geycC9kp-dq4Mw3A&oe=62F7BDFE".toUrl(),
        favicon = "https://static.xx.fbcdn.net/rsrc.php/v3/yw/r/HTE9u6HBvgx.png".toUrl(),
      )
    }
  ),
  Gitless_com(  // Does not use any social tags.
    url = "/gitless.com",
    htmlFileName = "html_source_gitless.com.html",
    expected = { serverUrl ->
      UnfurlResult(
        url = "https://gitless.com".toUrl(),
        title = "Gitless",
        description = "Gitless: a simple version control system built on top of Git",
        thumbnail = null,
        favicon = "${serverUrl}favicon.ico".toUrl(),
      )
    }
  )
}

fun readResourceFile(fileName: String): String {
  // TODO: Read from resources to run tests
  return ""
//  val url = Thread.currentThread().contextClassLoader.getResource(fileName)!!
//  return File(url.path).readText()
}

fun <T> parameterizedTest(parameters: Array<T>, testFunc: (T) -> Unit) {
  parameters.forEach {
    testFunc(it)
  }
}
