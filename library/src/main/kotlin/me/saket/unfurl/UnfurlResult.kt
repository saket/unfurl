package me.saket.unfurl

import okhttp3.HttpUrl

/**
 * @param url This URL might differ from the original URL used with [Unfurler.unfurl],
 * if HTTP 3xx redirects were followed. For instance, `https://youtu.be/foo` might
 * redirect to `https://www.youtube.com/watch?v=foo`.
 */
data class UnfurlResult(
  val url: HttpUrl,
  val title: String?,
  val description: String?,
  val favicon: HttpUrl?,
  val thumbnail: HttpUrl?,
  val contentPreview: ContentPreview? = null,
) {

  /**
   * Additional metadata that can be populated by extensions.
   * See `TweetContentPreview` for an example.
   */
  interface ContentPreview
}
