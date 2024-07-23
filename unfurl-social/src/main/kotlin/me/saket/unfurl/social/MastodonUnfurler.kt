package me.saket.unfurl.social

import com.squareup.moshi.Moshi
import me.saket.unfurl.UnfurlResult
import me.saket.unfurl.extension.UnfurlerExtension
import me.saket.unfurl.extension.UnfurlerScope
import okhttp3.HttpUrl

/**
 * TODO: doc.
 *
 * FYI rate-limiting isn't handled yet.
 */
class MastodonUnfurler : UnfurlerExtension {
  private val moshi = Moshi.Builder().build()

  override suspend fun UnfurlerScope.unfurl(url: HttpUrl): UnfurlResult? {
    val tweetId = TweetLinkRegex.parseTweetId(url) ?: return null
    return null
  }

  companion object {
    fun isTweetUrl(url: HttpUrl): Boolean {
      return TweetLinkRegex.parseTweetId(url) != null
    }
  }
}

internal object TweetLinkRegex {
  private val regex = Regex("^/(?:\\w+)/status/(?<id>[\\w\\d]+)\$")

  fun parseTweetId(url: HttpUrl): String? {
    if (!url.host.contains("twitter.com")) {
      return null
    }

    val result = regex.find(url.encodedPath) ?: return null
    return result.groups["id"]?.value ?: return null
  }
}
