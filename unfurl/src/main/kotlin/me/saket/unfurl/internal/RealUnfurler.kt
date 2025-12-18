package me.saket.unfurl.internal

import me.saket.unfurl.UnfurlLogger
import me.saket.unfurl.UnfurlResult
import me.saket.unfurl.Unfurler
import me.saket.unfurl.extension.HtmlMetadataUnfurlerExtension
import me.saket.unfurl.extension.UnfurlerExtension
import me.saket.unfurl.extension.UnfurlerScope
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import kotlin.coroutines.cancellation.CancellationException

internal class RealUnfurler(
  cacheSize: Int,
  extensions: List<UnfurlerExtension>,
  private val httpClient: OkHttpClient,
  private val logger: UnfurlLogger,
) : Unfurler {
  private val extensions = extensions + HtmlMetadataUnfurlerExtension()
  private val cache = NullableLruCache<String, UnfurlResult?>(cacheSize)

  private val extensionScope = object : UnfurlerScope {
    override val httpClient: OkHttpClient get() = this@RealUnfurler.httpClient
    override val logger: UnfurlLogger get() = this@RealUnfurler.logger
  }

  override suspend fun unfurl(url: String): UnfurlResult? {
    return cache.computeIfAbsent(url) {
      url.toHttpUrlOrNull()?.let { httpUrl ->
        extensions.firstNotNullOfOrNull { extension ->
          extension.unfurlSafely(httpUrl)
        }
      }
    }
  }

  private suspend fun UnfurlerExtension.unfurlSafely(url: HttpUrl): UnfurlResult? {
    try {
      return extensionScope.unfurl(url)
    } catch (e: Throwable) {
      if (e is CancellationException) {
        throw e
      } else {
        val extension: UnfurlerExtension = this
        logger.log(e, "Failed to unfurl '$url' using $extension")
        return null
      }
    }
  }
}
