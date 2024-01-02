package me.saket.unfurl

import io.ktor.client.HttpClient
import io.ktor.http.Url
import me.saket.unfurl.extension.HtmlTagsBasedUnfurler
import me.saket.unfurl.extension.UnfurlerExtension
import me.saket.unfurl.extension.UnfurlerScope
import me.saket.unfurl.extension.toHttpUrlOrNull
import me.saket.unfurl.internal.NullableLruCache

public class Unfurler(
  cacheSize: Int = 100,
  extensions: List<UnfurlerExtension> = emptyList(),
  public val httpClient: HttpClient = defaultHttpClient(),
  public val logger: UnfurlLogger = UnfurlLogger.Println,
) {
  private val extensions = extensions + HtmlTagsBasedUnfurler()
  private val cache = NullableLruCache<String, UnfurlResult?>(cacheSize)

  private val extensionScope = object : UnfurlerScope {
    override val httpClient: HttpClient get() = this@Unfurler.httpClient
    override val logger: UnfurlLogger get() = this@Unfurler.logger
  }

  public fun unfurl(url: String): UnfurlResult? {
    return cache.computeIfAbsent(url) {
      try {
        url.toHttpUrlOrNull()?.let { httpUrl ->
          extensions.asSequence()
            .mapNotNull { it.run { extensionScope.unfurl(httpUrl) } }
            .firstOrNull()
        }
      } catch (e: Throwable) {
        logger.log(e, "Failed to unfurl '$url'")
        null
      }
    }
  }

  public fun unfurl(url: Url): UnfurlResult? {
    return unfurl(url.toString())
  }

  public companion object {
    public fun defaultHttpClient(): HttpClient {
      return HttpClient(provideHttpClientEngine()) {
        followRedirects = true
      }
    }
  }
}
