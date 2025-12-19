@file:Suppress("DEPRECATION")

package me.saket.unfurl.extension

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import me.saket.unfurl.UnfurlResult
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.Response
import okhttp3.coroutines.executeAsync
import java.io.IOException
import kotlin.time.Duration.Companion.milliseconds
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
  private val httpUserAgents: List<String> = DefaultUserAgents,
) : UnfurlerExtension {

  @Suppress("unused")
  constructor(httpUserAgent: String) : this(listOf(httpUserAgent))

  override suspend fun UnfurlerScope.unfurl(url: HttpUrl): UnfurlResult? {
    return withContext(Dispatchers.IO) {
      downloadHtml(url)?.let { doc ->
        extractMetadata(doc)
      }
    }
  }

  protected suspend fun UnfurlerScope.downloadHtml(url: HttpUrl): JsoupDocument? {
    return winnerTakesItAll {
      for ((index, userAgent) in httpUserAgents.withIndex()) {
        attempt {
          if (index > 0) {
            // Most web pages should be reachable using the first user agent. Delay fallback
            // user agents to give the first one a chance to succeed instead of firing all at once.
            delay(500.milliseconds)
          }
          requestHtml(url, userAgent)?.use { response ->
            if (response.isSuccessful && response.body.contentType().isHtmlText()) {
              claimVictory {
                parseHtml(response)
              }
            }
          }
        }
      }
    }
  }

  private suspend fun UnfurlerScope.requestHtml(url: HttpUrl, userAgent: String): Response? {
    logger.log("Downloading HTML for $url using user agent: $userAgent")

    val request: Request = Request.Builder()
      .url(url)
      .header("User-Agent", userAgent)
      .header("Accept", "text/html")
      .header("Accept-Language", "en-US,en;q=0.5")
      .build()

    try {
      return httpClient.newCall(request).executeAsync()
    } catch (e: IOException) {
      logger.log(e, "Failed to download HTML for $url using user agent: $userAgent")
      return null
    }
  }

  private fun parseHtml(response: Response): JsoupDocument {
    val jsoup = JsoupStreamParser(JsoupParser.htmlParser())
    jsoup.parse(
      /* input = */ response.body.charStream().buffered(),
      /* baseUri = */ response.request.url.toString(),
    )
    jsoup.use {
      // Note to self: selectFirst() parses the stream until it finds the <head> block.
      // Its return value is discarded because HtmlMetadataParser requires the entire document.
      // Fortunately, the document is built lazily, so Jsoup doesn't download the rest of the HTML.
      it.selectFirst("head")
      return it.document()
    }
  }

  protected open fun UnfurlerScope.extractMetadata(document: JsoupDocument): UnfurlResult? {
    val parser = HtmlMetadataParser(logger)
    return parser.parse(url = document.baseUri().toHttpUrl(), document = document)
  }

  private fun MediaType?.isHtmlText(): Boolean {
    return this != null && type == "text" && subtype == "html"
  }

  @Suppress("unused")
  @Deprecated("htmlByteLimit is no longer used. HTML downloads now automatically stop once the <head> tag is received.")
  constructor(
    httpUserAgent: String = DefaultUserAgents.first(),
    htmlByteLimit: Long = -1,
  ) : this(listOf(httpUserAgent))

  @Suppress("ConstPropertyName", "unused")
  companion object {
    val DefaultUserAgents: List<String> = listOf(
      WhatsAppUserAgent,
      SlackBotUserAgent,
      ChromeMobileUserAgent,
    )

    const val SlackBotUserAgent =
      "Slackbot-LinkExpanding 1.0 (+https://api.slack.com/robots)"

    const val ChromeMobileUserAgent =
      "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Mobile Safari/537.36"

    // Also used by Signal.
    const val WhatsAppUserAgent =
      "WhatsApp/2"
  }
}
