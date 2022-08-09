package me.saket.unfurl

import me.saket.unfurl.delegates.UnfurlerDelegate
import me.saket.unfurl.delegates.UnfurlerDelegateScope
import me.saket.unfurl.delegates.html.HtmlTagsBasedUnfurler
import me.saket.unfurl.internal.NullableLruCache
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient

class Unfurler(
  cacheSize: Int = 100,
  delegates: List<UnfurlerDelegate> = emptyList(),
  private val httpClient: OkHttpClient = defaultOkHttpClient(),
  private val logger: UnfurlLogger = UnfurlLogger.Println
) {
  private val delegates = delegates + HtmlTagsBasedUnfurler()
  private val cache = NullableLruCache<String, UnfurlResult?>(cacheSize)

  fun unfurl(url: String): UnfurlResult? {
    return cache.computeIfAbsent(url) {
      try {
        with (unfurlerDelegateScope()) {
          url.toHttpUrlOrNull()?.let { httpUrl ->
            delegates.asSequence()
              .mapNotNull { it.run { unfurl(httpUrl) } }
              .firstOrNull()
          }
        }
      } catch (e: Throwable) {
        logger.log(e, "Failed to unfurl '$url'")
        null
      }
    }
  }

  private fun unfurlerDelegateScope(): UnfurlerDelegateScope {
    return object : UnfurlerDelegateScope {
      override val httpClient: OkHttpClient get() = this@Unfurler.httpClient
      override val logger: UnfurlLogger get() = this@Unfurler.logger
    }
  }

  companion object {
    fun defaultOkHttpClient(): OkHttpClient {
      return OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
    }
  }
}
