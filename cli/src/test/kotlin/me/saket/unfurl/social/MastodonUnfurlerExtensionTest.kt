package me.saket.unfurl.social

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThanOrEqualTo
import assertk.assertions.isNull
import kotlinx.coroutines.test.runTest
import me.saket.unfurl.Unfurler
import me.saket.unfurl.cmd.extensions.MastodonRegexes
import me.saket.unfurl.cmd.extensions.MastodonStatusId
import me.saket.unfurl.cmd.extensions.MastodonUnfurlerExtension
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Test

class MastodonUnfurlerExtensionTest {
  @Test fun `status with text`() = runTest {
    val unfurler = Unfurler(
      extensions = listOf(MastodonUnfurlerExtension())
    )

    val unfurled = unfurler.unfurl("https://androiddev.social/@saket/112731877340552402")!!
    with(unfurled) {
      assertThat(url).isEqualTo("https://androiddev.social/@saket/112731877340552402".toHttpUrl())
      assertThat(title).isEqualTo("@saket")
      assertThat(description).isEqualTo("<p>Finding it very hard to keep up with my OSS projects when summer is summering this hard</p>")
      with(extra(MastodonUnfurlerExtension.EngagementStatsExtra::class)!!) {
        assertThat(favorites).isGreaterThanOrEqualTo(13)
        assertThat(replies).isGreaterThanOrEqualTo(3)
        assertThat(boosts).isEqualTo(0)
      }
    }
  }

  @Test fun `correctly parse status IDs using regex`() {
    fun parse(link: String) = MastodonRegexes.parseStatusId(link.toHttpUrl())

    assertThat(parse("https://androiddev.social/")).isNull()
    assertThat(parse("https://androiddev.social/@saket/")).isNull()
    assertThat(parse("https://androiddev.social/@saket/112731877340552402")).isEqualTo(
      MastodonStatusId(
        host = "https://androiddev.social".toHttpUrl(),
        id = "112731877340552402",
      )
    )
  }
}
