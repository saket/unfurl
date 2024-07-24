@file:OptIn(ExperimentalStdlibApi::class)

package me.saket.unfurl.cmd.extensions

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import me.saket.unfurl.UnfurlResult
import me.saket.unfurl.extension.HtmlTagsBasedUnfurler
import me.saket.unfurl.extension.UnfurlerExtension
import me.saket.unfurl.extension.UnfurlerScope
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request as HttpRequest

class MastodonUnfurlerExtension : UnfurlerExtension {
  private val moshi = Moshi.Builder().build()

  override suspend fun UnfurlerScope.unfurl(url: HttpUrl): UnfurlResult? {
    val statusId = MastodonRegexes.parseStatusId(url) ?: return null

    val request = HttpRequest.Builder()
      .url(
        statusId.host.newBuilder()
          .encodedPath("/api/v1/statuses/${statusId.id}")
          .build()
      )
      .build()

    try {
      httpClient.newCall(request).execute().use { response ->
        response.body?.let { body ->
          val status = moshi.adapter<MastodonStatus>()
            .fromJson(body.source())!!
            .thisOrReblogged()

          val htmlTags = with(HtmlTagsBasedUnfurler()) { unfurl(url) }

          return UnfurlResult(
            url = status.url!!.toHttpUrl(),
            title = "@${status.account.acct}",
            description = status.content,
            thumbnail = htmlTags?.thumbnail,
            favicon = htmlTags?.favicon,
            extras = mapOf(
              EngagementStatsExtra::class to EngagementStatsExtra(
                favorites = status.favourites_count,
                replies = status.replies_count,
                boosts = status.reblogs_count,
              ),
            ),
          )
        }
      }
    } catch (e: Throwable) {
      logger.log(e, "Failed to parse status: $url")
    }
    return null
  }

  data class EngagementStatsExtra(
    val favorites: Int,
    val replies: Int,
    val boosts: Int,
  )
}

internal object MastodonRegexes {
  private val UsernameRegex = Regex("^@[\\w.-]+(@[\\w.-]+)?$")
  private val StatusIdRegex = Regex("^[0-9]+$")

  fun parseStatusId(httpUrl: HttpUrl): MastodonStatusId? {
    return if (isPossiblyAStatusUrl(httpUrl)) {
      MastodonStatusId(
        host = httpUrl.toString().substringBefore(httpUrl.encodedPath).toHttpUrl(),
        id = httpUrl.pathSegments[1],
      )
    } else {
      null
    }
  }

  private fun isPossiblyAStatusUrl(httpUrl: HttpUrl): Boolean {
    val urlSegments = httpUrl.pathSegments
    return urlSegments.size >= 2
      && urlSegments[0].matches(UsernameRegex)
      && urlSegments[1].matches(StatusIdRegex)
  }
}

internal data class MastodonStatusId(
  val host: HttpUrl,
  val id: String,
)
