package me.saket.unfurl.delegates.html

import me.saket.unfurl.UnfurlResult
import me.saket.unfurl.delegates.UnfurlerDelegate
import me.saket.unfurl.delegates.UnfurlerDelegateScope
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class HtmlTagsBasedUnfurler(
  parsers: List<HtmlMetadataParser> = emptyList(),
) : UnfurlerDelegate {
  private val parsers = parsers + DefaultHtmlMetadataParser()

  override fun UnfurlerDelegateScope.unfurl(url: HttpUrl): UnfurlResult? {
    return downloadHtml(url)?.extractMetadata()
  }

  private fun UnfurlerDelegateScope.downloadHtml(url: HttpUrl): Document? {
    val request: Request = Request.Builder()
      .url(url)
      .apply {
        if (url.host.contains("twitter.com")) {
          // Masquerade as a Googlebot to prevent Twitter from using javascript to load their data
          // asynchronously. It is preferred to use TweetUnfurler whenever possible to avoid this.
          header("User-Agent", "Mozilla/5.0 (compatible; Googlebot/2.1; +http://google.com/bot.html)")
        }
      }
      .build()

    return try {
      httpClient.newCall(request).execute().use { response ->
        val body = response.body
        val redirectedUrl = response.request.url

        if (body != null && body.contentType().isHtmlText()) {
          // TODO: stream the HTML body only until a "</head>" is received instead of streaming the entire HTML body.
          Jsoup.parse(
            /* in */ body.source().inputStream(),
            /* charsetName */ null,
            /* baseUri */ redirectedUrl.toString()
          )
        } else {
          null
        }
      }
    } catch (e: Throwable) {
      logger.log(e.stackTraceToString())
      null
    }
  }

  private fun Document.extractMetadata(): UnfurlResult? {
    return parsers.asSequence()
      .map { it.parse(url = baseUri().toHttpUrl(), document = this) }
      .firstOrNull()
  }

  private fun MediaType?.isHtmlText(): Boolean {
    return this != null && type == "text" && subtype == "html"
  }
}
