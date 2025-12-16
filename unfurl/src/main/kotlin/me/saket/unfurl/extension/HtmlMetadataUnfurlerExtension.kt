package me.saket.unfurl.extension

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import me.saket.unfurl.UnfurlResult
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType
import okhttp3.Request
import org.jsoup.Jsoup
import ru.gildor.coroutines.okhttp.await
import org.jsoup.nodes.Document as JsoupDocument

/**
 * The default extension used by [Unfurler][me.saket.unfurl.Unfurler] for unfurling URLs using
 * their HTML metadata.
 *
 * @param httpUserAgents List of `User-Agent` HTTP headers to try when fetching HTML. Multiple user agents
 *   are attempted concurrently, and the first successful response is used. This is useful because some
 *   websites may block or return different content based on the User-Agent.
 * @param htmlByteLimit Maximum number of bytes to download from the HTML page. This is used in the
 *   HTTP Range header to limit bandwidth usage, assuming that metadata tags (like Open Graph tags)
 *   are typically present in the initial portion of the HTML. May not be supported by all websites.
 */
open class HtmlMetadataUnfurlerExtension(
  private val httpUserAgents: List<String>,
  private val htmlByteLimit: Long = 32_768,
) : UnfurlerExtension {

  @Suppress("unused")
  constructor(
    httpUserAgent: String = SlackBotUserAgent,
    htmlByteLimit: Long = 32_768,
  ) : this(listOf(httpUserAgent), htmlByteLimit)

  override suspend fun UnfurlerScope.unfurl(url: HttpUrl): UnfurlResult? {
    return downloadHtml(url)?.let { doc ->
      extractMetadata(doc)
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  protected suspend fun UnfurlerScope.downloadHtml(url: HttpUrl): JsoupDocument? {
    return httpUserAgents
      .map { userAgent -> flow { emit(downloadHtml(url, userAgent)) } }
      .merge()
      .filterNotNull()
      .firstOrNull()
  }

  private suspend fun UnfurlerScope.downloadHtml(url: HttpUrl, userAgent: String): JsoupDocument? {
    logger.log("Downloading HTML for $url using user agent: $userAgent")

    val request: Request = Request.Builder()
      .url(url)
      .header("User-Agent", userAgent)
      .header("Accept", "text/html")
      .header("Accept-Language", "en-US,en;q=0.5")
      // Fetch as little of the page as possible, hoping
      // that the HTML tags are present in the initial range.
      // This was copied from Slack.
      .header("Range", "bytes=0-$htmlByteLimit")
      .build()

    return try {
      httpClient.newCall(request).await().use { response ->
        val body = response.body
        val redirectedUrl = response.request.url

        if (response.isSuccessful && body != null && body.contentType().isHtmlText()) {
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
      logger.log(e, "Failed to download HTML for $url using user agent: $userAgent")
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

  @Suppress("ConstPropertyName", "unused")
  companion object {
    const val SlackBotUserAgent =
      "Slackbot-LinkExpanding 1.0 (+https://api.slack.com/robots)"

    const val ChromeMobileUserAgent =
      "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/127.0.0.0 Mobile Safari/537.36"

    // Also used by Signal.
    const val WhatsAppUserAgent =
      "WhatsApp/2"

    val DefaultUserAgents: List<String> = listOf(
      WhatsAppUserAgent,
      SlackBotUserAgent,
      ChromeMobileUserAgent,
    )
  }
}
