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

  open fun UnfurlerScope.extractMetadata(document: Document): UnfurlResult? {
    val parser = HtmlMetadataParser(logger)
    return parser.parse(url = document.baseUri().toHttpUrl(), document = document)
  }

  private fun MediaType?.isHtmlText(): Boolean {
    return this != null && type == "text" && subtype == "html"
  }
}
