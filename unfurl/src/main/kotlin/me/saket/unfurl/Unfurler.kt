package me.saket.unfurl

import me.saket.unfurl.extension.HtmlTagsBasedUnfurler
import me.saket.unfurl.extension.UnfurlerExtension
import me.saket.unfurl.extension.UnfurlerExtensionScope
import me.saket.unfurl.internal.NullableLruCache
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient

class Unfurler(
  cacheSize: Int = 100,
  delegates: List<UnfurlerExtension> = emptyList(),
  val httpClient: OkHttpClient = defaultOkHttpClient(),
  val logger: UnfurlLogger = UnfurlLogger.Println
) {
  private val delegates = delegates + HtmlTagsBasedUnfurler()
  private val cache = NullableLruCache<String, UnfurlResult?>(cacheSize)

  private val extensionScope = object : UnfurlerExtensionScope {
    override val httpClient: OkHttpClient get() = this@Unfurler.httpClient
    override val logger: UnfurlLogger get() = this@Unfurler.logger
  }

  fun unfurl(url: String): UnfurlResult? {
    return cache.computeIfAbsent(url) {
      try {
        url.toHttpUrlOrNull()?.let { httpUrl ->
          delegates.asSequence()
            .mapNotNull { it.run { extensionScope.unfurl(httpUrl) } }
            .firstOrNull()
        }
      } catch (e: Throwable) {
        logger.log(e, "Failed to unfurl '$url'")
        null
      }
    }
  }

  fun unfurl(url: HttpUrl): UnfurlResult? {
    return unfurl(url.toString())
  }

  companion object {
    internal fun defaultOkHttpClient(): OkHttpClient {
      return OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
    }
  }
}
