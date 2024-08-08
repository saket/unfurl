package me.saket.unfurl.extension

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.saket.unfurl.UnfurlResult
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document as JsoupDocument

open class HtmlTagsBasedUnfurler : UnfurlerExtension {
  override suspend fun UnfurlerScope.unfurl(url: HttpUrl): UnfurlResult? {
    return withContext(Dispatchers.IO) {
      downloadHtml(url)?.let { doc ->
        extractMetadata(doc)
      }
    }
  }

  @Suppress("MemberVisibilityCanBePrivate")
  protected fun UnfurlerScope.downloadHtml(url: HttpUrl): JsoupDocument? {
    val request: Request = Request.Builder()
      .url(url)
      // Some websites will deny empty/unknown user agents,
      // probably in an attempt to prevent scrapers?
      .header(
        "User-Agent",
        "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/117.0.0.0 Mobile Safari/537.36"
      )
      // Websites like nitter will deny requests if
      // content type and language headers are missing.
      .header("Accept", "text/html")
      .header("Accept-Language", "en-US,en;q=0.5")
      .build()

    return try {
      httpClient.newCall(request).execute().use { response ->
        val body = response.body
        val redirectedUrl = response.request.url

        if (body != null && body.contentType().isHtmlText()) {
          Jsoup.parse(
            /* in */ body.source().inputStream(),
            /* charsetName */ null,
            /* baseUri */ redirectedUrl.toString(),
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

  protected open fun UnfurlerScope.extractMetadata(document: JsoupDocument): UnfurlResult? {
    val parser = HtmlMetadataParser(logger)
    return parser.parse(url = document.baseUri().toHttpUrl(), document = document)
  }

  private fun MediaType?.isHtmlText(): Boolean {
    return this != null && type == "text" && subtype == "html"
  }
}
