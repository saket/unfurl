package me.saket.unfurl

import me.saket.unfurl.delegates.UnfurlerDelegate
import okhttp3.OkHttpClient

@Deprecated(
  message = """"delegates" parameter has been renamed to "extensions"""",
  replaceWith = ReplaceWith(
    "Unfurler(cacheSize = cacheSize, extensions = delegates, httpClient = httpClient)",
    "me.saket.unfurl.Unfurler"
  ),
)
fun Unfurler(
  cacheSize: Int = 100,
  delegates: List<UnfurlerDelegate>,
  httpClient: OkHttpClient = Unfurler.defaultOkHttpClient(),
) = Unfurler(
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
fun Unfurler(
  cacheSize: Int = 100,
  delegates: List<UnfurlerDelegate>,
) = Unfurler(
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
fun Unfurler(
  delegates: List<UnfurlerDelegate>
) = Unfurler(
  extensions = delegates,
)
