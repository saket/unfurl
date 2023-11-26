package me.saket.unfurl.extension

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.ported.BufferReader
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.url
import io.ktor.client.statement.readBytes
import io.ktor.client.statement.request
import io.ktor.http.ContentType
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.utils.io.core.use
import kotlinx.coroutines.runBlocking
import me.saket.unfurl.UnfurlResult

public open class HtmlTagsBasedUnfurler : UnfurlerExtension {
  override fun UnfurlerScope.unfurl(url: Url): UnfurlResult? {
    return downloadHtml(url)?.let { doc ->
      extractMetadata(doc)
    }
  }

  private fun UnfurlerScope.downloadHtml(url: Url): Document? {
    return try {
      httpClient.use {
        runBlocking {
          val response = it.request {
            header(
              "User-Agent",
              "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/117.0.0.0 Mobile Safari/537.36"
            )
            url(url)
            build()
          }

          val contentType = response.contentType()
          val redirectedUrl = response.request.url

          if (contentType.isHtmlText()) {
            Ksoup.parse(
              /* in */ BufferReader(response.readBytes()),
              /* charsetName */ null,
              /* baseUri */ redirectedUrl.toString()
            )
          } else {
            null
          }
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

  private fun ContentType?.isHtmlText(): Boolean {
    return this != null && contentType == "text" && contentSubtype == "html"
  }
}
