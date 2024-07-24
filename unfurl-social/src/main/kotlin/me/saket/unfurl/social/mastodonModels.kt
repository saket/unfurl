package me.saket.unfurl.social

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class MastodonStatus(
  val replies_count: Int,
  val reblogs_count: Int,
  val favourites_count: Int,
  val content: String,
  val account: MastodonAccount,
  val reblog: MastodonStatus?,
  val url: String?, // Null for reblogs.
) {
  fun thisOrReblogged(): MastodonStatus {
    return reblog ?: this
  }
}

@JsonClass(generateAdapter = true)
internal data class MastodonAccount(
  val acct: String, // Webfinger username
)
