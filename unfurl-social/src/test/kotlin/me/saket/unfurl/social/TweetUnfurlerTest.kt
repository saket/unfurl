package me.saket.unfurl.social

import com.google.common.truth.Truth.assertThat
import me.saket.unfurl.Unfurler
import me.saket.unfurl.social.TweetContentPreview.AttachedImage
import me.saket.unfurl.social.TweetContentPreview.AttachedVideo
import me.saket.unfurl.social.TweetContentPreview.AttachedVideo.VideoVariant
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import org.junit.Test
import java.time.ZonedDateTime

class TweetUnfurlerTest {
  private fun unfurler(twitter: TweetUnfurler? = tweetUnfurler()): Unfurler {
    return Unfurler(extensions = listOfNotNull(twitter))
  }

  private fun tweetUnfurler(): TweetUnfurler {
    val token = System.getenv("unfurler_twitter_token") ?: error("missing api token")
    return TweetUnfurler(bearerToken = token)
  }

  @Test fun `ignore non-twitter links`() {
    val unfurled = unfurler(twitter = null).unfurl("https://dog.ceo")
    assertThat(unfurled).isNull()
  }

  @Test fun `tweet with text`() {
    val unfurled = unfurler().unfurl("https://twitter.com/ElspethEastman/status/777579297515057152")
    assertThat(unfurled?.contentPreview).isEqualTo(
      TweetContentPreview(
        authorUsername = "ElspethEastman",
        authorProfileName = "Elspeth Eastman",
        authorProfilePhoto = "https://pbs.twimg.com/profile_images/1554229677417500672/Y-SzhJBP.jpg".toHttpUrl(),
        createdAt = ZonedDateTime.parse("2016-09-18T18:45:25Z"),
        body = """
          |Avocado: not ripe
          |Avocado: not ripe
          |Avocado: not ripe
          |Avocado: I'M RIPE NOW
          |Avocado: okay you were in the bathroom so I rotted
          """.trimMargin(),
        attachments = emptyList(),
      )
    )
  }

  @Test fun `tweet with one image`() {
    val unfurled = unfurler().unfurl("https://twitter.com/new_cheats_news/status/1556727895778856960")
    assertThat(unfurled?.contentPreview).isEqualTo(
      TweetContentPreview(
        authorUsername = "new_cheats_news",
        authorProfileName = "Unlisted Cheats",
        authorProfilePhoto = "https://pbs.twimg.com/profile_images/504699161720020992/6XoUBgH5.png".toHttpUrl(),
        createdAt = ZonedDateTime.parse("2022-08-08T19:43:47Z"),
        body = "While doing some request work, casually found some new cheats in Super Punch-out for SNES. " +
          "All secret codes in this game uses two-button combinations. Two of them are known: Sound test and " +
          "Japanese name input. But there are two more. ->",
        attachments = listOf(
          AttachedImage("https://pbs.twimg.com/media/FZqaZkTXgAQg5qG.png".toHttpUrl())
        )
      )
    )
  }

  @Test fun `tweet with one video`() {
    val unfurled = unfurler().unfurl("https://twitter.com/RonFilipkowski/status/1552092292537880576")
    assertThat(unfurled?.contentPreview).isEqualTo(
      TweetContentPreview(
        authorUsername = "RonFilipkowski",
        authorProfileName = "Ron Filipkowski 🇺🇦",
        authorProfilePhoto = "https://pbs.twimg.com/profile_images/1416869035996758020/R0cKz3Gc.jpg".toHttpUrl(),
        createdAt = ZonedDateTime.parse("2022-07-27T00:43:33.000Z"),
        body = "OAN host makes an urgent plea to liberals to help save the network after Verizon dropped " +
          "them: “It is absolutely crucial that, for once, we defy the powers that be, we all come together," +
          " set aside our differences in a unified effort.”",
        attachments = listOf(
          AttachedVideo(
            previewImage = "https://pbs.twimg.com/ext_tw_video_thumb/1552092211986169856/pu/img/JMAJJKF6Sa81iQTO.jpg".toHttpUrl(),
            variants = listOf(
              VideoVariant(
                bitRate = 832_000,
                mediaType = "video/mp4".toMediaType(),
                url = "https://video.twimg.com/ext_tw_video/1552092211986169856/pu/vid/504x360/Lqq2kUVVk5XK2tpS.mp4?tag=14".toHttpUrl()
              ),
              VideoVariant(
                bitRate = 256_000,
                mediaType = "video/mp4".toMediaType(),
                url = "https://video.twimg.com/ext_tw_video/1552092211986169856/pu/vid/378x270/zGC9BhN_HJe-DsP6.mp4?tag=14".toHttpUrl()
              ),
              VideoVariant(
                bitRate = 2_176_000,
                mediaType = "video/mp4".toMediaType(),
                url = "https://video.twimg.com/ext_tw_video/1552092211986169856/pu/vid/692x494/dDPNYxUCfnCQqt7u.mp4?tag=14".toHttpUrl()
              ),
              VideoVariant(
                bitRate = null,
                mediaType = "application/x-mpegURL".toMediaType(),
                url = "https://video.twimg.com/ext_tw_video/1552092211986169856/pu/pl/v84I2BENS9QsDtSL.m3u8?tag=14&container=fmp4".toHttpUrl()
              )
            )
          )
        )
      )
    )
  }

  @Test fun `tweet with multiple images`() {
    val unfurled = unfurler().unfurl("https://twitter.com/alex_albon/status/1554829547006111749?s=21")
    assertThat(unfurled?.contentPreview).isEqualTo(
      TweetContentPreview(
        authorUsername = "alex_albon",
        authorProfileName = "Alex Albon",
        authorProfilePhoto = "https://pbs.twimg.com/profile_images/1515250261345845254/YXCd7tXy.jpg".toHttpUrl(),
        createdAt = ZonedDateTime.parse("2022-08-03T14:00:25.000Z"),
        body = """
          |I understand that, with my agreement, Williams Racing have put out a press release this afternoon that I am driving for them next year. This is right and I have signed a contract with Williams for 2023. I will be driving for Williams next year.
          |
          |😂 let’s gooo @williamsracing 💪
          """.trimMargin(),
        attachments = listOf(
          AttachedImage("https://pbs.twimg.com/media/FZPdOjNUcAMcirV.jpg".toHttpUrl()),
          AttachedImage("https://pbs.twimg.com/media/FZPdOjZVsAUtFjf.jpg".toHttpUrl()),
          AttachedImage("https://pbs.twimg.com/media/FZPdOjRUYAAUpcI.jpg".toHttpUrl()),
        )
      )
    )
  }

  @Test fun `tweet with a link`() {
    val unfurled = unfurler().unfurl("https://mobile.twitter.com/refsrc/status/1548981583234736128")
    assertThat(unfurled?.contentPreview).isEqualTo(
      TweetContentPreview(
        authorUsername = "refsrc",
        authorProfileName = "Manish Singh",
        authorProfilePhoto = "https://pbs.twimg.com/profile_images/1531735655889244160/s2nOBgZH.jpg".toHttpUrl(),
        createdAt = ZonedDateTime.parse("2022-07-18T10:42:42.000Z"),
        body = "Almost half a trillion dollars has been wiped from the valuation of once high-flying " +
          "financial tech companies that took advantage of the boom in initial public offerings earlier " +
          "in the pandemic. https://t.co/Reil6g51Tn",
        attachments = listOf()
      )
    )
  }

  @Test fun retweet() {
    val unfurled = unfurler().unfurl("https://mobile.twitter.com/refsrc/status/1555528336331247620")
    assertThat(unfurled?.contentPreview).isEqualTo(
      TweetContentPreview(
        authorUsername = "refsrc",
        authorProfileName = "Manish Singh",
        authorProfilePhoto = "https://pbs.twimg.com/profile_images/1531735655889244160/s2nOBgZH.jpg".toHttpUrl(),
        createdAt = ZonedDateTime.parse("2022-08-05T12:17:10.000Z"),
        body = "Five years after Apple's partner Foxconn began assembling iPhone units in India, cobbling " +
          "together older generation handsets, they seem ready / willing to produce the flagship model for " +
          "the first time in the country. https://t.co/CaIVgjrDPr",
        attachments = listOf()
      )
    )
  }

  @Test fun `correctly parse tweet IDs using regex`() {
    fun parse(link: String): String? = TweetLinkRegex.parseTweetId(link.toHttpUrl())

    assertThat(parse("https://twitter.com/AndroidPolice/status/1556642538269904898?s=20&t=Z09MNQXpX1C8MOv3-BiiSg"))
      .isEqualTo("1556642538269904898")

    assertThat(parse("https://mobile.twitter.com/refsrc/status/1548981583234736128")).isEqualTo("1548981583234736128")
    assertThat(parse("https://twitter.com/saketme/status/1414341015289352192")).isEqualTo("1414341015289352192")
    assertThat(parse("https://twitter.com/i/events/1556813714740850690")).isNull()
    assertThat(parse("https://twitter.com/explore")).isNull()
  }
}
