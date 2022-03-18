package me.saket.unfurl.delegates.html

import me.saket.unfurl.UnfurlResult
import me.saket.unfurl.delegates.UnfurlerDelegate
import me.saket.unfurl.delegates.UnfurlerDelegateScope
import okhttp3.HttpUrl
import okhttp3.MediaType
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class HtmlTagsBasedUnfurler(
  parsers: List<HtmlMetadataParser> = emptyList(),
) : UnfurlerDelegate {
  private val parsers = parsers + DefaultHtmlMetadataParser()

  override fun UnfurlerDelegateScope.unfurl(url: HttpUrl): UnfurlResult? {
    return downloadHtml(url)?.extractMetadata(url)
  }

  private fun UnfurlerDelegateScope.downloadHtml(url: HttpUrl): Document? {
    val request: Request = Request.Builder()
      .url(url)
      .build()

    return try {
      httpClient.newCall(request).execute().use { response ->
        val body = response.body
        if (body != null && body.contentType().isHtmlText()) {
          // TODO: stream the HTML body only until a "</head>" is received instead of streaming the entire HTML body.
          Jsoup.parse(
            /* in */ body.source().inputStream(),
            /* charsetName */ null,
            /* baseUri */ url.toString()
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

  private fun Document.extractMetadata(url: HttpUrl): UnfurlResult? {
    return parsers.asSequence()
      .map { it.parse(url, document = this) }
      .firstOrNull()
  }

  private fun MediaType?.isHtmlText(): Boolean {
    return this != null && type == "text" && subtype == "html"
  }
}
