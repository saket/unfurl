@file:Suppress("JUnitMalformedDeclaration")

package me.saket.unfurl

import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(TestParameterInjector::class)
class UnfurlerTest {
  private val server = MockWebServer()
  private val unfurler = Unfurler()

  @Test fun unfurl(@TestParameter input: TestInput) {
    server.enqueue(
      MockResponse()
        .setHeader("Content-Type", "text/html; charset=UTF-8")
        .setBody(readResourceFile(input.htmlFileName))
    )

    server.use {
      val localUrl = server.url(input.url)
      val result = unfurler.unfurl(localUrl)
      assertThat(result).isEqualTo(input.expected(server.url(""))?.copy(url = localUrl))
      assertThat(server.takeRequest()).isNotNull()
    }
  }

  @Test fun `unfurl tweets without javascript`() {
    // This does not use a mock web-server to ensure my workaround for scraping tweets is still working.
    val result = unfurler.unfurl("https://twitter.com/Twitter/status/1577730467436138524")

    assertThat(result).isEqualTo(
      UnfurlResult(
        url = "https://twitter.com/Twitter/status/1577730467436138524".toHttpUrl(),
        title = "Twitter on Twitter",
        description = """
              |“whoa, it works
              |
              |now everyone can mix GIFs, videos, and images in one Tweet, available on iOS and Android”
              """.trimMargin(),
        thumbnail = "https://pbs.twimg.com/tweet_video_thumb/FeU5fh1XkA0vDAE.jpg:large".toHttpUrl(),
        favicon = "https://abs.twimg.com/favicons/twitter.2.ico".toHttpUrl(),
      )
    )
  }

  @Test fun `follow redirects`() {
    server.enqueue(
      MockResponse()
        .setResponseCode(303)
        .setHeader("Location", "https://www.youtube.com/watch?v=o-YBDTqX_ZU&feature=youtu.be")
    )

    server.use {
      val result = unfurler.unfurl(server.url("/youtu.be/o-YBDTqX_ZU"))
      assertThat(result).isNotNull()
      assertThat(server.takeRequest()).isNotNull()
    }
  }
}

@Suppress("EnumEntryName", "unused")
enum class TestInput(
  val url: String,
  val htmlFileName: String,
  val expected: (serverUrl: HttpUrl) -> UnfurlResult?
) {
  Saket_me( // Uses both OGP and twitter meta tags.
    url = "/saket.me/great-teams-merge-fast/",
    htmlFileName = "html_source_saket.me.html",
    expected = {
      UnfurlResult(
        url = "https://saket.me/great-teams-merge-fast/".toHttpUrl(),
        title = "Great teams merge fast",
        description = "Observations from watching my team at Square produce stellar work while moving fast and not breaking things.",
        thumbnail = "https://saket.me/wp-content/uploads/2021/02/great_teams_merge_fast_cover.jpg".toHttpUrl(),
        favicon = "https://saket.me/wp-content/uploads/2022/03/cropped-saket-photo-180x180.jpg".toHttpUrl(),
      )
    }
  ),
  Instagram_com(  // Does not use most twitter meta tags.
    url = "/about.instagram.com",
    htmlFileName = "html_source_instagram.com.html",
    expected = {
      UnfurlResult(
        url = "https://about.instagram.com/".toHttpUrl(),
        title = "About Instagram's Official Site",
        description = "We strive to bring people together in a safe and supportive community. We believe expression is the greatest connector. Make the most of your Instagram experience!",
        thumbnail = "https://scontent-ort2-2.xx.fbcdn.net/v/t39.2365-6/75883158_790065824784383_3063578611500974080_n.jpg?_nc_cat=109&ccb=1-7&_nc_sid=ad8a9d&_nc_ohc=AIIVl_p_K0gAX9iad2X&_nc_ht=scontent-ort2-2.xx&oh=00_AT-0skyUQDSPTdFriyws79pzJ1z1Z9geycC9kp-dq4Mw3A&oe=62F7BDFE".toHttpUrl(),
        favicon = "https://static.xx.fbcdn.net/rsrc.php/v3/yw/r/HTE9u6HBvgx.png".toHttpUrl(),
      )
    }
  ),
  Gitless_com(  // Does not use any social tags.
    url = "/gitless.com",
    htmlFileName = "html_source_gitless.com.html",
    expected = { serverUrl ->
      UnfurlResult(
        url = "https://gitless.com".toHttpUrl(),
        title = "Gitless",
        description = "Gitless: a simple version control system built on top of Git",
        thumbnail = null,
        favicon = "${serverUrl}favicon.ico".toHttpUrl(),
      )
    }
  )
}

fun readResourceFile(fileName: String): String {
  val url = Thread.currentThread().contextClassLoader.getResource(fileName)!!
  return File(url.path).readText()
}
