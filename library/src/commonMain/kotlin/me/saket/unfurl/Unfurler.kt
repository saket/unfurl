package me.saket.unfurl

import io.ktor.client.HttpClient
import me.saket.unfurl.Unfurler.Companion.defaultHttpClient
import me.saket.unfurl.extension.HtmlTagsBasedUnfurler
import me.saket.unfurl.extension.UnfurlerExtension
import me.saket.unfurl.extension.UnfurlerScope
import me.saket.unfurl.internal.NullableLruCache
import me.saket.unfurl.internal.RealUnfurler
import me.saket.unfurl.internal.toUrlOrNull

public interface Unfurler {
  public suspend fun unfurl(url: String): UnfurlResult?

  public companion object {
    public fun defaultHttpClient(): HttpClient {
      return HttpClient(provideHttpClientEngine()) {
        followRedirects = true
      }
    }
  }
}

public fun Unfurler(
  cacheSize: Int = 100,
  extensions: List<UnfurlerExtension> = emptyList(),
  httpClient: HttpClient = defaultHttpClient(),
  logger: UnfurlLogger = UnfurlLogger.Println,
): Unfurler {
  return RealUnfurler(
    cacheSize = cacheSize,
    extensions = extensions,
    httpClient = httpClient,
    logger = logger
  )
}
