package me.saket.unfurl.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.animation.textAnimation
import com.github.ajalt.mordant.markdown.Markdown
import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.rendering.TextColors.blue
import com.github.ajalt.mordant.table.table
import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.saket.unfurl.UnfurlLogger
import me.saket.unfurl.UnfurlResult
import me.saket.unfurl.Unfurler
import me.saket.unfurl.social.TweetContentPreview
import me.saket.unfurl.social.TweetContentPreview.AttachedImage
import me.saket.unfurl.social.TweetContentPreview.AttachedVideo
import me.saket.unfurl.social.TweetUnfurler
import me.saket.unfurl.social.highestQuality
import okhttp3.HttpUrl
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

  private val terminal = Terminal()
  private val maxWidthOfTableColumn = 52

  override fun run() = runBlocking {
    val url = url.toHttpUrlOrNull()
    if (url == null) {
      echo("Invalid link", err = true)
      return@runBlocking
    }

    val unfurler = Unfurler(
      extensions = listOfNotNull(twitterToken?.let(::TweetUnfurler)),
      logger = if (debug) UnfurlLogger.Println else UnfurlLogger.NoOp
    )

    val unfurled = withProgressAnimation { unfurler.unfurl(url) }
    if (unfurled == null) {
      echo("Couldn't unfurl", err = true)
    } else {
      echo("")
      when (val content = unfurled.contentPreview) {
        is TweetContentPreview -> printTweet(content)
        null -> printGenericLink(unfurled)
      }
    }
  }

  private suspend fun <T> withProgressAnimation(block: () -> T): T {
    val frames = "⣾⣽⣻⢿⡿⣟⣯⣷"
    val animation = terminal.textAnimation<Int> { frame ->
      blue(frames[frame % frames.length].toString())
    }

    return coroutineScope {
      val job = launch(IO) {
        terminal.cursor.hide(showOnExit = true)
        repeat(Int.MAX_VALUE) { frame ->
          animation.update(frame)
          delay(100)
        }
      }
      job.invokeOnCompletion {
        animation.clear()
        terminal.cursor.show()
      }

      return@coroutineScope block().also {
        job.cancel()
      }
    }
  }

  private fun printTweet(tweet: TweetContentPreview) {
    terminal.println(
      table {
        body {
          row("Author", "${tweet.authorProfileName} (@${tweet.authorUsername})")
          row("Photo", tweet.authorProfilePhoto?.ellipsizeAndHyperlink())
          row("Tweet", tweet.body.breakLines())
          row("Timestamp", tweet.createdAt.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)))

          if (tweet.attachments.isNotEmpty()) {
            val urls = tweet.attachments.map { attachment ->
              when (attachment) {
                is AttachedImage -> attachment.url
                is AttachedVideo -> attachment.variants.highestQuality().url
                else -> error("unsupported attachment: $attachment")
              }
            }
            row {
              cell("Attachments") { rowSpan = urls.size }
              cell(urls.first().ellipsizeAndHyperlink())
            }
            urls.drop(1).forEach {
              row(it.ellipsizeAndHyperlink())
            }
          }
        }
      }
    )
  }

  private fun printGenericLink(unfurled: UnfurlResult) {
    val wasUrlExpanded = url.removeSuffix("/") != unfurled.url.toString().removeSuffix("/")

    terminal.println(
      table {
        body {
          if (wasUrlExpanded) {
            row("URL", unfurled.url.ellipsizeAndHyperlink())
          }
          row("Title", unfurled.title?.breakLines())
          row("Description", unfurled.description?.breakLines())
          row("Thumbnail", unfurled.thumbnail?.ellipsizeAndHyperlink())
          row("Favicon", unfurled.favicon?.ellipsizeAndHyperlink())
        }
      }
    )
  }

  private fun String.breakLines(): String {
    return chunked(maxWidthOfTableColumn).joinToString(separator = "\n")
  }

  private fun HttpUrl.ellipsizeAndHyperlink(): Markdown {
    val ellipsized = toString().let {
      if (it.length > maxWidthOfTableColumn) "${it.take(maxWidthOfTableColumn - 1)}…" else it
    }
    return Markdown(
      markdown = "[$ellipsized](${toString()})",
      hyperlinks = true,
    )
  }
}
