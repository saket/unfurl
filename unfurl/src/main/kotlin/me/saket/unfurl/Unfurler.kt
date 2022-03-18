@file:Suppress("NAME_SHADOWING")

package me.saket.unfurl

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.saket.unfurl.delegates.UnfurlerDelegate
import me.saket.unfurl.delegates.UnfurlerDelegateScope
import me.saket.unfurl.delegates.html.HtmlTagsBasedUnfurler
import me.saket.unfurl.internal.NullableLruCache
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import kotlin.coroutines.CoroutineContext

class Unfurler(
  cacheSize: Int = 100,
  delegates: List<UnfurlerDelegate> = emptyList(),
  override val dispatcher: CoroutineContext = Dispatchers.IO,
  override val httpClient: OkHttpClient = OkHttpClient.Builder().build(),
  override val logger: Logger = PrintlnLogger
) : UnfurlerDelegateScope {

  private val delegates = delegates + HtmlTagsBasedUnfurler()
  private val cache = NullableLruCache<String, UnfurlResult?>(cacheSize)

  suspend fun unfurl(url: String): UnfurlResult? {
    return cache.computeIfAbsent(url) {
      withContext(dispatcher) {
        try {
          url.toHttpUrlOrNull()?.let { httpUrl ->
            delegates.asSequence()
              .mapNotNull { it.run { unfurl(httpUrl) } }
              .firstOrNull()
          }
        } catch (e: Throwable) {
          logger.log("Failed to unfurl '$url'")
          logger.log(e.stackTraceToString())
          null
        }
      }
    }
  }
}
