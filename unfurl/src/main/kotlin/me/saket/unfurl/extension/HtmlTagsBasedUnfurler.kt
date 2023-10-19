package me.saket.unfurl.extension

import me.saket.unfurl.UnfurlResult
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

open class HtmlTagsBasedUnfurler : UnfurlerExtension {
  override fun UnfurlerScope.unfurl(url: HttpUrl): UnfurlResult? {
    return downloadHtml(url)?.let { doc ->
      extractMetadata(doc)
    }
  }

  private fun UnfurlerScope.downloadHtml(url: HttpUrl): Document? {
    val request: Request = Request.Builder()
      .url(url)
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
      logger.log(e, "Failed to download HTML for $url")
      null
    }
  }

  open fun UnfurlerScope.extractMetadata(document: Document): UnfurlResult? {
    val parser = HtmlMetadataParser(logger)
    return parser.parse(url = document.baseUri().toHttpUrl(), document = document)
  }

  private fun MediaType?.isHtmlText(): Boolean {
    return this != null && type == "text" && subtype == "html"
  }
}
