package me.saket.unfurl

import io.ktor.client.HttpClient
import me.saket.unfurl.delegates.UnfurlerDelegate

@Deprecated(
  message = """"delegates" parameter has been renamed to "extensions"""",
  replaceWith = ReplaceWith(
    "Unfurler(cacheSize = cacheSize, extensions = delegates, httpClient = httpClient)",
    "me.saket.unfurl.Unfurler"
  ),
)
public fun Unfurler(
  cacheSize: Int = 100,
  delegates: List<UnfurlerDelegate>,
  httpClient: HttpClient = Unfurler.defaultHttpClient(),
): Unfurler = Unfurler(
  cacheSize = cacheSize,
  extensions = delegates,
  httpClient = httpClient,
)

@Deprecated(
  message = """"delegates" parameter has been renamed to "extensions"""",
  replaceWith = ReplaceWith(
    "Unfurler(cacheSize = cacheSize, extensions = delegates)",
    "me.saket.unfurl.Unfurler"
  ),
)
public fun Unfurler(
  cacheSize: Int = 100,
  delegates: List<UnfurlerDelegate>,
): Unfurler = Unfurler(
  cacheSize = cacheSize,
  extensions = delegates,
)

@Deprecated(
  message = """"delegates" parameter has been renamed to "extensions"""",
  replaceWith = ReplaceWith(
    "Unfurler(extensions = delegates)",
    "me.saket.unfurl.Unfurler"
  ),
)
public fun Unfurler(
  delegates: List<UnfurlerDelegate>
): Unfurler = Unfurler(
  extensions = delegates,
)
