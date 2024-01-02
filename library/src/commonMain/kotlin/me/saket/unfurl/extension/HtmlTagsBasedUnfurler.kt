package me.saket.unfurl.extension

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.ported.BufferReader
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.readBytes
import io.ktor.client.statement.request
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import me.saket.unfurl.UnfurlResult
import me.saket.unfurl.internal.toUrl

public open class HtmlTagsBasedUnfurler : UnfurlerExtension {
  override fun UnfurlerScope.unfurl(url: Url): UnfurlResult? {
    return downloadHtml(url)?.let { doc ->
      extractMetadata(doc)
    }
  }

  private fun UnfurlerScope.downloadHtml(url: Url): Document? {
    return try {
      runBlocking {
        val response = httpClient.get(url) {
          header(
            HttpHeaders.UserAgent,
            "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/117.0.0.0 Mobile Safari/537.36"
          )
          header(HttpHeaders.Accept, "text/html")
          header(HttpHeaders.AcceptLanguage, "en-US,en;q=0.5")
        }
        if (response.contentType() == ContentType.Text.Html) {
          Ksoup.parse(
            bufferReader = BufferReader(response.readBytes()),
            baseUri = response.request.url.toString(),
            charsetName = null,
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

  public open fun UnfurlerScope.extractMetadata(document: Document): UnfurlResult? {
    val parser = HtmlMetadataParser(logger)
    return parser.parse(url = document.baseUri().toUrl(), document = document)
  }
}
