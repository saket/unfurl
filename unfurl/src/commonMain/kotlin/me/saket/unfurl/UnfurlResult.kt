package me.saket.unfurl

import io.ktor.http.Url

/**
 * @param url May or may not be equal to the original URL used with [Unfurler.unfurl].
 * This can happen in situations where HTTP 3xx redirects are followed. For example,
 * `https://youtu.be/foo` will redirect to `https://www.youtube.com/watch?v=foo`.
 */
public data class UnfurlResult(
  val url: Url,
  val title: String?,
  val description: String?,
  val favicon: Url?,
  val thumbnail: Url?,
  val contentPreview: ContentPreview? = null,
) {

  /**
   * Additional metadata that can be populated by extensions.
   * See `TweetContentPreview` for an example.
   */
  public interface ContentPreview
}
