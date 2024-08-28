package me.saket.unfurl.extension

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.io.SourceReader
import com.fleeksoft.ksoup.io.SourceReaderImpl
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.InternalAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.saket.unfurl.UnfurlLogger
import me.saket.unfurl.UnfurlResult
import okhttp3.HttpUrl

open class HtmlMetadataUnfurlerExtension(
  private val httpUserAgent: String = SlackBotUserAgent,
  private val htmlByteLimit: Long = 32_768,
) : UnfurlerExtension {

  override suspend fun UnfurlerScope.unfurl(url: HttpUrl): UnfurlResult? {
    return withContext(Dispatchers.IO) {
      downloadHtmlAndExtractMetadata(url)
    }
  }

  private suspend fun downloadHtmlAndExtractMetadata(url: HttpUrl): UnfurlResult? {
    val ktor = HttpClient(OkHttp.create()) {
      followRedirects = true
    }

    val response = ktor.get(url.toString()) {
      // Some websites will deny empty/unknown user agents,
      // probably in an attempt to prevent scrapers?
      header(HttpHeaders.UserAgent, httpUserAgent)
      // Websites like nitter will deny requests if
      // content type and language headers are missing.
      header(HttpHeaders.Accept, "text/html")
      header(HttpHeaders.AcceptLanguage, "en-US,en;q=0.5")
      // Fetch as little of the page as possible, hoping
      // that the HTML tags are present in the initial range.
      // This was copied from Slack.
      header("Range", "bytes=0-$htmlByteLimit")
    }
    if (response.contentType()?.match(ContentType.Text.Html) == true) {
      val doc = Ksoup.parse(
        sourceReader = SourceReader(response.bodyAsChannel()),
        baseUri = url.toString(),
      )

      val metadataParser = HtmlMetadataParser(UnfurlLogger.println())
      return metadataParser.parse(url, doc)
    } else {
      return null
    }
  }

  @Suppress("ConstPropertyName", "unused")
  companion object {
    // Unfurl uses Slack's user agent by default because websites may
    // have special handling for slack. Source: https://api.slack.com/robots.
    const val SlackBotUserAgent = "Slackbot-LinkExpanding 1.0 (+https://api.slack.com/robots)"

    const val ChromeMobileUserAgent =
      "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/127.0.0.0 Mobile Safari/537.36"
  }
}

// Can be removed once https://github.com/fleeksoft/ksoup/issues/50#issuecomment-2314097347 is released.
@OptIn(InternalAPI::class)
private fun SourceReader(channel: ByteReadChannel): SourceReader {
  return SourceReaderImpl(channel.readBuffer)
}
