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

open class HtmlMetadataUnfurlerExtension(
  private val httpUserAgent: String = SlackBotUserAgent,
  private val htmlByteLimit: Long = 32_768,
) : UnfurlerExtension {

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
      .header("User-Agent", httpUserAgent)
      // Websites like nitter will deny requests if
      // content type and language headers are missing.
      .header("Accept", "text/html")
      .header("Accept-Language", "en-US,en;q=0.5")
      // Fetch as little of the page as possible, hoping
      // that the HTML tags are present in the initial range.
      // This was copied from Slack.
      .header("Range", "bytes=0-$htmlByteLimit")
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

  companion object {
    // Unfurl uses Slack's user agent by default because websites may
    // have special handling for slack. Source: https://api.slack.com/robots.
    const val SlackBotUserAgent = "Slackbot-LinkExpanding 1.0 (+https://api.slack.com/robots)"

    const val ChromeMobileUserAgent =
      "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/127.0.0.0 Mobile Safari/537.36"
  }
}
