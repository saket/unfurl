package me.saket.unfurl

import okhttp3.HttpUrl

/**
 * @param url May or may not be equal to the original URL used with [Unfurler.unfurl].
 * This can happen in situations where HTTP 3xx redirects are followed. For example,
 * `https://youtu.be/foo` will redirect to `https://www.youtube.com/watch?v=foo`.
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
   * Additional meta data that can be populated by extensions.
   * See `TweetContentPreview` for an example.
   */
  interface ContentPreview
}
