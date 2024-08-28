package me.saket.unfurl

import me.saket.unfurl.extension.UnfurlerExtension
import me.saket.unfurl.internal.RealUnfurler
import okhttp3.HttpUrl
import okhttp3.OkHttpClient

fun Unfurler(
  cacheSize: Int = 100,
  extensions: List<UnfurlerExtension> = emptyList(),
  httpClient: OkHttpClient = Unfurler.defaultOkHttpClient(),
  logger: UnfurlLogger = UnfurlLogger.println(),
): Unfurler {
  return RealUnfurler(
    cacheSize = cacheSize,
    extensions = extensions,
    httpClient = httpClient,
    logger = logger,
  )
}

interface Unfurler {
  suspend fun unfurl(url: String): UnfurlResult?

  companion object;
}

suspend fun Unfurler.unfurl(url: HttpUrl): UnfurlResult? {
  return unfurl(url.toString())
}

fun Unfurler.Companion.defaultOkHttpClient(): OkHttpClient {
  return OkHttpClient.Builder()
    .followRedirects(true)
    .followSslRedirects(true)
    .build()
}
