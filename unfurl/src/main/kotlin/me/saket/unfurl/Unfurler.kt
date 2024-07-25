package me.saket.unfurl

import me.saket.unfurl.Unfurler.Companion.defaultOkHttpClient
import me.saket.unfurl.extension.UnfurlerExtension
import me.saket.unfurl.internal.RealUnfurler
import okhttp3.HttpUrl
import okhttp3.OkHttpClient

interface Unfurler {
  suspend fun unfurl(url: String): UnfurlResult?

  suspend fun unfurl(url: HttpUrl): UnfurlResult? {
    return unfurl(url.toString())
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

fun Unfurler(
  cacheSize: Int = 100,
  extensions: List<UnfurlerExtension> = emptyList(),
  httpClient: OkHttpClient = defaultOkHttpClient(),
  logger: UnfurlLogger = UnfurlLogger.println(),
): Unfurler {
  return RealUnfurler(
    cacheSize = cacheSize,
    extensions = extensions,
    httpClient = httpClient,
    logger = logger
  )
}
