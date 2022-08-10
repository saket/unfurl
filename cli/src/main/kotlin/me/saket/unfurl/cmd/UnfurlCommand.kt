package me.saket.unfurl.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import me.saket.unfurl.UnfurlLogger
import me.saket.unfurl.UnfurlResult
import me.saket.unfurl.Unfurler
import me.saket.unfurl.social.TweetContentPreview
import me.saket.unfurl.social.TweetContentPreview.AttachedImage
import me.saket.unfurl.social.TweetContentPreview.AttachedVideo
import me.saket.unfurl.social.TweetUnfurler
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

fun main(args: Array<String>) {
  UnfurlCommand().main(args)
}

class UnfurlCommand : CliktCommand(name = "unfurl") {
  private val url: String by argument("url")
  private val twitterToken: String? by option("-t", "--twitter-token", envvar = "unfurler_twitter_token")
  private val debug: Boolean by option().flag(default = false)

  override fun run() {
    val url = url.toHttpUrlOrNull()
    if (url == null) {
      echo("Invalid link", err = true)
      return
    }

    val unfurler = Unfurler(
      logger = if (debug) UnfurlLogger.Println else UnfurlLogger.NoOp,
      extensions = listOfNotNull(
        twitterToken?.let { TweetUnfurler(bearerToken = it) }
      )
    )

    val unfurled = unfurler.unfurl(url)
    if (unfurled == null) {
      echo("Couldn't unfurl", err = true)
    } else {
      echo("")
      if (url.host != unfurled.url.host && url.encodedPath != unfurled.url.encodedPath) {
        echo("Url: ${this.url}")
      }
      when (val content = unfurled.contentPreview) {
        is TweetContentPreview -> printTweet(content)
        null -> printGenericLink(unfurled)
      }
    }
  }

  private fun printTweet(tweet: TweetContentPreview) {
    echo("Author: ${tweet.authorProfileName} (@${tweet.authorUsername})")
    echo("Tweet: ${tweet.body}")
    echo("Timestamp: ${tweet.createdAt.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM))}")

    if (tweet.attachments.isNotEmpty()) {
      echo("\nMedia: ")
      tweet.attachments.forEachIndexed { index, attachment ->
        val mediaUrl = when (attachment) {
          is AttachedImage -> attachment.url
          is AttachedVideo -> attachment.variants.sortedByDescending { it.bitRate }.firstOrNull()?.url
          else -> error("unsupported attachment: $attachment")
        }
        echo("[$index] $mediaUrl")
      }
    }
  }

  private fun printGenericLink(unfurled: UnfurlResult) {
    with(unfurled) {
      when (title) {
        null -> echo("Title: null")
        else -> echo("Title: \"$title\"")
      }
      when (description) {
        null -> echo("Title: null")
        else -> echo("Description: \"$description\"")
      }
      echo("Thumbnail: $thumbnail")
      echo("Favicon: $favicon")
    }
  }
}
