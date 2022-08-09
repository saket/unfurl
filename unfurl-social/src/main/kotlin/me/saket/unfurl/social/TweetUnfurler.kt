@file:OptIn(ExperimentalStdlibApi::class)

package me.saket.unfurl.social

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import me.saket.unfurl.UnfurlResult
import me.saket.unfurl.delegates.html.HtmlTagsBasedUnfurler
import me.saket.unfurl.extension.UnfurlerExtension
import me.saket.unfurl.extension.UnfurlerExtensionScope
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Because Twitter uses javascript for rendering its webpages, [HtmlTagsBasedUnfurler]
 * isn't able to extract all metadata of tweets. TweetUnfurler can be used as an alternative.
 *
 * FYI rate-limiting isn't handled yet.
 *
 * @param bearerToken can be obtained by signing up for a developer account at
 * [https://developer.twitter.com/en/docs/twitter-api/getting-started/getting-access-to-the-twitter-api].
 */
class TweetUnfurler(private val bearerToken: String) : UnfurlerExtension {
  private val regex = Regex("^/(?:\\w+)/status/([\\w\\d]+)\$")
  private val moshi = Moshi.Builder().build()

  override fun UnfurlerExtensionScope.unfurl(url: HttpUrl): UnfurlResult? {
    if (!url.host.contains("twitter.com")) {
      return null
    }
    val tweetId = regex.find(url.encodedPath)?.groupValues?.getOrNull(1)
      ?: return null

    val request = okhttp3.Request.Builder()
      .url(
        "https://api.twitter.com/2/tweets".toHttpUrl()
          .newBuilder()
          .addQueryParameter("ids", tweetId)
          .addQueryParameter("expansions", "author_id")
          .addQueryParameter("user.fields", "name,profile_image_url")
          .build()
      )
      .header("Authorization", "Bearer $bearerToken")
      .build()

    try {
      httpClient.newCall(request).execute().use { response ->
        response.body?.let { body ->
          val tweet = moshi.adapter<TweetResponse>().fromJson(body.source())!!
          val user = tweet.includes.users.firstOrNull()
          return UnfurlResult(
            url = url,
            title = user?.name,
            description = tweet.data.firstOrNull()?.text,
            thumbnail = user?.profile_image_url?.fullSizedImage()?.toHttpUrlOrNull(),
            // Latest favicon can be found on https://developer.twitter.com/en/docs/twitter-for-websites/web-intents/image-resources.
            favicon = "https://cdn.cms-twdigitalassets.com/content/dam/developer-twitter/images/Twitter_logo_blue_48.png".toHttpUrl()
          )
        }
      }
    } catch (e: Throwable) {
      logger.log(e.stackTraceToString())
    }
    return null
  }

  /**
   * Twitter sends tiny images by default, but the URL can be changed to fetch their original size.
   * [Source](https://developer.twitter.com/en/docs/twitter-api/v1/accounts-and-users/user-profile-images-and-banners).
   */
  private fun String.fullSizedImage(): String {
    return if (endsWith("_normal.jpg")) {
      "${substringBefore("_normal.jpg")}.jpg"
    } else {
      this
    }
  }

  @JsonClass(generateAdapter = true)
  internal data class TweetResponse(
    val data: List<Data>,
    val includes: Includes
  ) {
    @JsonClass(generateAdapter = true)
    data class Data(
      val text: String
    )

    @JsonClass(generateAdapter = true)
    data class Includes(
      val users: List<User>
    )

    @JsonClass(generateAdapter = true)
    data class User(
      val profile_image_url: String,
      val name: String
    )
  }
}
