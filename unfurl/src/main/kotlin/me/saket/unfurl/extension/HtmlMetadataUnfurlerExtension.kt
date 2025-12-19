@file:Suppress("DEPRECATION")

package me.saket.unfurl.extension

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.withContext
import me.saket.unfurl.UnfurlResult
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.Response
import okhttp3.coroutines.executeAsync
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

  @Deprecated(
    "htmlByteLimit is no longer used. Unfurler automatically stops downloading HTML once the <head> tag is received."
  )
  @Suppress("unused")
  constructor(
    httpUserAgent: String = DefaultUserAgents.first(),
    htmlByteLimit: Long = -1,
  ) : this(listOf(httpUserAgent))

  override suspend fun UnfurlerScope.unfurl(url: HttpUrl): UnfurlResult? {
    return withContext(Dispatchers.IO) {
      downloadHtml(url)?.let { doc ->
        extractMetadata(doc)
      }
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  protected suspend fun UnfurlerScope.downloadHtml(url: HttpUrl): JsoupDocument? {
    val httpResponse = httpUserAgents
      .mapIndexed { index, userAgent ->
        flow {
          if (index > 0) {
            // Most web pages should be reachable using the first user agent. Delay fallback
            // user agents to give the first one a chance to succeed instead of firing all at once.
            delay(500.milliseconds)
          }
          emit(fetchHtmlResponse(url, userAgent))
        }
      }
      .merge()
      .filterNotNull()
      .firstOrNull()
    return httpResponse?.extractHtml()
  }

  private suspend fun UnfurlerScope.fetchHtmlResponse(url: HttpUrl, userAgent: String): Response? {
    logger.log("Downloading HTML for $url using user agent: $userAgent")

    val request: Request = Request.Builder()
      .url(url)
      .header("User-Agent", userAgent)
      .header("Accept", "text/html")
      .header("Accept-Language", "en-US,en;q=0.5")
      .build()

    try {
      val response = httpClient.newCall(request).executeAsync()
      val contentType = response.body.contentType()
      if (response.isSuccessful && contentType.isHtmlText()) {
        return response
      } else {
        logger.log(
          "Failed to download HTML for $url using user agent: $userAgent. " +
            "Received HTTP status: ${response.code}, Content-Type: $contentType."
        )
        return null
      }
    } catch (e: Throwable) {
      if (e is CancellationException) {
        throw e
      } else {
        logger.log(e, "Failed to download HTML for $url using user agent: $userAgent")
        return null
      }
    }
  }

  private fun Response.extractHtml(): JsoupDocument {
    this.use { response ->
      val jsoup = JsoupStreamParser(JsoupParser.htmlParser())
      jsoup.parse(
        /* input = */ response.body.charStream().buffered(),
        /* baseUri = */ response.request.url.toString(),
      )
      jsoup.use { jsoup ->
        // Note to self: selectFirst() parses the stream until it finds the <head> block.
        // Its return value is discarded because HtmlMetadataParser requires the entire document.
        // Fortunately, the document is built lazily, so Jsoup doesn't download the rest of the HTML.
        jsoup.selectFirst("head")
        return jsoup.document()
      }
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
