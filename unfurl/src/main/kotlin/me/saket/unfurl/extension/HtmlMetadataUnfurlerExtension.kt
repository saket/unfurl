@file:Suppress("DEPRECATION")

package me.saket.unfurl.extension

import kotlinx.coroutines.CancellationException
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
import okhttp3.coroutines.executeAsync
import org.jsoup.nodes.Document as JsoupDocument
import org.jsoup.parser.Parser as JsoupParser
import org.jsoup.parser.StreamParser as JsoupStreamParser

/**
 * The default extension used by [Unfurler][me.saket.unfurl.Unfurler] for unfurling URLs using
 * their HTML metadata.
 *
 * @param httpUserAgents List of `User-Agent` HTTP headers to try when fetching HTML. Multiple user agents
 *   are attempted concurrently, and the first successful response is used. This is useful because some
 *   websites may block or return different content based on the User-Agent.
 */
open class HtmlMetadataUnfurlerExtension(
  private val httpUserAgents: List<String>,
) : UnfurlerExtension {

  @Deprecated(
    "htmlByteLimit is no longer used. Unfurler automatically stops downloading HTML once the <head> tag is received."
  )
  @Suppress("unused")
  constructor(
    httpUserAgent: String = SlackBotUserAgent,
    htmlByteLimit: Long = -1,
  ) : this(listOf(httpUserAgent))

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
      .build()

    return try {
      httpClient.newCall(request).executeAsync().use { response ->
        val body = response.body
        val redirectedUrl = response.request.url

        if (response.isSuccessful && body.contentType().isHtmlText()) {
          val jsoup = JsoupStreamParser(JsoupParser.htmlParser())
          jsoup.parse(body.charStream().buffered(), /* baseUri = */ redirectedUrl.toString())
          jsoup.use { jsoup ->
            // Note to self: selectFirst() parses the stream until it finds the <head> block.
            // Its return value is discarded because HtmlMetadataParser requires the entire document.
            // Fortunately, the document is built lazily, so Jsoup doesn't download the rest of the HTML.
            jsoup.selectFirst("head")
            jsoup.document()
          }
        } else {
          null
        }
      }
    } catch (e: Throwable) {
      if (e !is CancellationException) {
        logger.log(e, "Failed to download HTML for $url using user agent: $userAgent")
      }
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
      "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Mobile Safari/537.36"

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
