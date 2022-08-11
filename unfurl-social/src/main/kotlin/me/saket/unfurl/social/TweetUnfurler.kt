@file:OptIn(ExperimentalStdlibApi::class)

package me.saket.unfurl.social

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import me.saket.unfurl.UnfurlResult
import me.saket.unfurl.delegates.html.HtmlTagsBasedUnfurler
import me.saket.unfurl.extension.UnfurlerExtension
import me.saket.unfurl.extension.UnfurlerScope
import me.saket.unfurl.social.TweetContentPreview.AttachedImage
import me.saket.unfurl.social.TweetContentPreview.AttachedVideo
import me.saket.unfurl.social.TweetContentPreview.AttachedVideo.VideoVariant
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import java.time.ZonedDateTime
import org.jsoup.parser.Parser as JsoupParser

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
  private val moshi = Moshi.Builder().build()

  override fun UnfurlerScope.unfurl(url: HttpUrl): UnfurlResult? {
    val tweetId = TweetLinkRegex.parseTweetId(url) ?: return null
    val request = okhttp3.Request.Builder()
      .url(
        "https://api.twitter.com/2/tweets".toHttpUrl()
          .newBuilder()
          .addQueryParameter("ids", tweetId)
          .addQueryParameter("expansions", "author_id,attachments.media_keys")
          .addQueryParameter("user.fields", "name,profile_image_url")
          .addQueryParameter("tweet.fields", "created_at,entities")
          .addQueryParameter("media.fields", "url,preview_image_url,variants")
          .build()
      )
      .header("Authorization", "Bearer $bearerToken")
      .build()

    try {
      httpClient.newCall(request).execute().use { response ->
        response.body?.let { body ->
          val tweet = moshi.adapter<TweetResponse>().fromJson(body.source())!!
          val data = tweet.data.first()
          val user = tweet.includes.users.first()
          val profilePhoto = user.profile_image_url.fullSizedImage().toHttpUrlOrNull()
          val tweetBody = removeSelfUrlsFromTweetBody(
            data = data,
            user = user,
            tweetId = tweetId,
            body = JsoupParser.unescapeEntities(data.text, /* inAttribute = */ true)
          )

          return UnfurlResult(
            url = url,
            title = user.name,
            description = tweetBody,
            thumbnail = profilePhoto,
            // Latest favicon can be found on https://developer.twitter.com/en/docs/twitter-for-websites/web-intents/image-resources.
            favicon = "https://cdn.cms-twdigitalassets.com/content/dam/developer-twitter/images/Twitter_logo_blue_48.png".toHttpUrl(),
            contentPreview = TweetContentPreview(
              authorProfileName = user.name,
              authorUsername = user.username,
              authorProfilePhoto = profilePhoto,
              createdAt = ZonedDateTime.parse(data.created_at),
              body = tweetBody,
              attachments = tweet.includes.media.orEmpty().mapNotNull {
                when (it.type) {
                  "photo" -> AttachedImage(
                    url = it.url!!.toHttpUrl()
                  )
                  "video" -> AttachedVideo(
                    previewImage = it.preview_image_url!!.toHttpUrl(),
                    variants = it.variants!!.map { variant ->
                      VideoVariant(
                        bitRate = variant.bit_rate,
                        mediaType = variant.content_type.toMediaType(),
                        url = variant.url.toHttpUrl()
                      )
                    }
                  )
                  else -> null
                }
              }
            )
          )
        }
      }
    } catch (e: Throwable) {
      logger.log(e, "Failed to parse tweet: $url")
    }
    return null
  }

  private fun removeSelfUrlsFromTweetBody(
    data: TweetResponse.Data,
    user: TweetResponse.User,
    tweetId: String,
    body: String
  ): String {
    val selfUrls = data.entities?.urls.orEmpty().filter { url ->
      val httpUrl = url.expanded_url.toHttpUrl()
      httpUrl.host == "twitter.com" && httpUrl.encodedPath.startsWith("/${user.username}/status/$tweetId")
    }
    return selfUrls.fold(body) { body, url ->
      val startIndex = body.indexOf(url.url)
      if (startIndex != -1) {
        val endIndex = startIndex + url.url.length
        val removeFrom = if (body.getOrNull(startIndex - 1) == ' ') startIndex - 1 else startIndex
        body.removeRange(removeFrom, endIndex)
      } else {
        body
      }
    }
  }

  /**
   * Twitter sends tiny images by default, but the URL can be changed to fetch their original size.
   * [Source](https://developer.twitter.com/en/docs/twitter-api/v1/accounts-and-users/user-profile-images-and-banners).
   */
  private fun String.fullSizedImage(): String {
    for (extension in arrayOf("jpg", "png")) {
      if (endsWith("_normal.$extension")) {
        return "${substringBefore("_normal.$extension")}.$extension"
      }
    }
    return this
  }

  companion object {
    fun isTweetUrl(url: HttpUrl): Boolean {
      return TweetLinkRegex.parseTweetId(url) != null
    }
  }
}

@JsonClass(generateAdapter = true)
internal data class TweetResponse(
  val data: List<Data>,
  val includes: Includes
) {
  @JsonClass(generateAdapter = true)
  data class Data(
    val text: String,
    val created_at: String,
    val entities: Entities?,
  )

  @JsonClass(generateAdapter = true)
  data class Entities(
    val urls: List<TweetUrl>,
  )

  @JsonClass(generateAdapter = true)
  data class TweetUrl(
    val url: String,
    val expanded_url: String,
  )

  @JsonClass(generateAdapter = true)
  data class Includes(
    val users: List<User>,
    val media: List<Media>?,
  )

  @JsonClass(generateAdapter = true)
  data class User(
    val profile_image_url: String,
    val name: String,
    val username: String,
  )

  @JsonClass(generateAdapter = true)
  data class Media(
    val type: String,
    val url: String?,                   // present only for images.
    val preview_image_url: String?,     // present only for videos.
    val variants: List<MediaVariants>?, // present only for videos.
  )

  @JsonClass(generateAdapter = true)
  data class MediaVariants(
    val bit_rate: Long?,  // null for adaptive video formats.
    val content_type: String,
    val url: String,
  )
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
