package me.saket.unfurl.extension

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.ported.BufferReader
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.request
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.http.charset
import io.ktor.http.contentType
import io.ktor.util.InternalAPI
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.pool.ByteArrayPool
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.runBlocking
import me.saket.unfurl.UnfurlResult
import me.saket.unfurl.internal.toUrl
import okio.Buffer
import okio.Source
import okio.Timeout
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

public open class HtmlTagsBasedUnfurler : UnfurlerExtension {
  override suspend fun UnfurlerScope.unfurl(url: Url): UnfurlResult? {
    return downloadHtml(url)?.let { doc ->
      extractMetadata(doc)
    }
  }

  @OptIn(InternalAPI::class)
  private suspend fun UnfurlerScope.downloadHtml(url: Url): Document? {
    return try {
      val response = httpClient.get(url) {
        header(
          HttpHeaders.UserAgent,
          "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/117.0.0.0 Mobile Safari/537.36"
        )
        header(HttpHeaders.Accept, "text/html")
        header(HttpHeaders.AcceptLanguage, "en-US,en;q=0.5")
      }
      if (response.contentType()?.match(ContentType.Text.Html) == true) {
        Ksoup.parse(
          bufferReader = BufferReader(source = response.content.toOkioSource()),
          baseUri = response.request.url.toString(),
          charsetName = response.charset()?.toString(),
        )
      } else {
        null
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

private fun ByteReadChannel.toOkioSource(context: CoroutineContext = EmptyCoroutineContext): Source {
  val channel = this
  return object : Source {
    private val byteArrayPool = ByteArrayPool

    override fun read(sink: Buffer, byteCount: Long): Long {
      val byteArray = byteArrayPool.borrow()
      val numOfBytesRead = runBlocking(context) {
        channel.readAvailable(byteArray)
      }
      sink.write(byteArray)
      byteArrayPool.recycle(byteArray)
      return numOfBytesRead.toLong()
    }

    override fun close() {
      channel.cancel(cause = null)
      byteArrayPool.close()
    }

    override fun timeout(): Timeout {
      return Timeout.NONE
    }
  }
}
