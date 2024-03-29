package me.saket.unfurl

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import com.goncalossilva.resources.Resource
import io.ktor.client.HttpClient
import io.ktor.client.engine.config
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondRedirect
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import me.saket.unfurl.internal.toUrl
import kotlin.test.Test

class UnfurlerTest {
  @Test fun `parse HTML correctly`() = runTest {
    HtmlTestInput.entries.forEach { input ->
      val mockEngine = MockEngine {
        respond(
          content = Resource("src/commonTest/resources/${input.htmlFileName}").readText(),
          status = HttpStatusCode.OK,
          headers = headersOf(HttpHeaders.ContentType, "text/html; charset=UTF-8")
        )
      }
      val unfurler = Unfurler(httpClient = HttpClient(mockEngine))
      val result = unfurler.unfurl(input.url)
      assertThat(result).isEqualTo(input.expected())
    }
  }

  @Test fun `websites that deny requests without a recognizable user-agent`() = runTest {
    val result = Unfurler().unfurl("https://www.getproactiv.ca/pdp?productcode=842944100695")
    assertThat(result).isEqualTo(
      UnfurlResult(
        url = "https://www.getproactiv.ca/pdp?productcode=842944100695".toUrl(),
        title = "Proactiv Solution® Repairing Treatment | Proactiv® Products",
        description = "Our Repairing Treatment is a leave-on treatment formulated with prescription-grade benzoyl peroxide designed to penetrate pores to kill acne-causing bacteria.",
        favicon = "https://www.getproactiv.ca/favicon.ico".toUrl(),
        thumbnail = "https://cdn-tp3.mozu.com/30113-50629/cms/50629/files/f050a010-0420-4a53-b898-d4c08db77eb9".toUrl(),
      )
    )
  }

  @Test fun `websites that deny requests without content type and language headers`() = runTest {
    val result = Unfurler().unfurl("https://nitter.net/saketme/status/1716330453311877183")
    assertThat(result).isEqualTo(
      UnfurlResult(
        url = "https://nitter.net/saketme/status/1716330453311877183".toUrl(),
        title = "saket@androiddev.social (@saketme)",
        description = "When the sole developer of a project starts using \"we\" instead of \"I\" in their code comments.",
        favicon = "https://nitter.net/apple-touch-icon.png".toUrl(),
        thumbnail = "https://nitter.net/pic/media%2FF9GhXLmXYAAXIcb.png".toUrl(),
      )
    )
  }

  @Test fun `follow redirects`() = runTest {
    val mockEngine = MockEngine.config {
      addHandler {
        when (it.url.host) {
          "youtu.be" -> {
            respondRedirect(
              location = "https://www.youtube.com/watch?v=o-YBDTqX_ZU&feature=youtu.be",
            )
          }
          "www.youtube.com" -> {
            respond(
              content = "",
              headers = headersOf(HttpHeaders.ContentType, ContentType.Text.Html.toString()),
            )
          }
          else -> error("unexpected url = ${it.url.host}")
        }
      }
    }
    val unfurler = Unfurler(httpClient = HttpClient(mockEngine))
    val result = unfurler.unfurl("https://youtu.be/o-YBDTqX_ZU")
    assertThat(result).isNotNull()
  }
}

@Suppress("EnumEntryName")
private enum class HtmlTestInput(
  val url: String,
  val htmlFileName: String,
  val expected: () -> UnfurlResult?,
) {
  Saket_me( // Uses both OGP and twitter meta tags.
    url = "https://saket.me/great-teams-merge-fast/",
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
    url = "https://about.instagram.com/",
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
    url = "https://gitless.com",
    htmlFileName = "html_source_gitless.com.html",
    expected = {
      UnfurlResult(
        url = "https://gitless.com".toUrl(),
        title = "Gitless",
        description = "Gitless: a simple version control system built on top of Git",
        thumbnail = null,
        favicon = "https://gitless.com/favicon.ico".toUrl(),
      )
    }
  )
}
