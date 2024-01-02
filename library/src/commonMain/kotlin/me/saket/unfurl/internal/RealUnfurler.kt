package me.saket.unfurl.internal

import io.ktor.client.HttpClient
import me.saket.unfurl.UnfurlLogger
import me.saket.unfurl.UnfurlResult
import me.saket.unfurl.Unfurler
import me.saket.unfurl.extension.HtmlTagsBasedUnfurler
import me.saket.unfurl.extension.UnfurlerExtension
import me.saket.unfurl.extension.UnfurlerScope

internal class RealUnfurler(
  cacheSize: Int,
  extensions: List<UnfurlerExtension>,
  private val httpClient: HttpClient,
  private val logger: UnfurlLogger,
) : Unfurler {
  private val extensions = extensions + HtmlTagsBasedUnfurler()
  private val cache = NullableLruCache<String, UnfurlResult?>(cacheSize)

  private val extensionScope = object : UnfurlerScope {
    override val httpClient: HttpClient get() = this@RealUnfurler.httpClient
    override val logger: UnfurlLogger get() = this@RealUnfurler.logger
  }

  override suspend fun unfurl(url: String): UnfurlResult? {
    return cache.computeIfAbsent(url) {
      try {
        url.toUrlOrNull()?.let { httpUrl ->
          extensions.firstNotNullOfOrNull {
            println("running extension: $it")
            it.run { extensionScope.unfurl(httpUrl) }
          }
        }
      } catch (e: Throwable) {
        logger.log(e, "Failed to unfurl '$url'")
        null
      }
    }
  }
}
