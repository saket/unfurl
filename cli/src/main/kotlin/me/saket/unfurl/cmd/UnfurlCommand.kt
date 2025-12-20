package me.saket.unfurl.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.animation.textAnimation
import com.github.ajalt.mordant.markdown.Markdown
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.table.table
import com.github.ajalt.mordant.terminal.ExperimentalTerminalApi
import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.saket.unfurl.UnfurlLogger
import me.saket.unfurl.UnfurlResult
import me.saket.unfurl.Unfurler
import me.saket.unfurl.cmd.extensions.MastodonUnfurlerExtension
import me.saket.unfurl.cmd.extensions.mastodonEngagementStats
import me.saket.unfurl.defaultOkHttpClient
import me.saket.unfurl.unfurl
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient

fun main(args: Array<String>) {
  UnfurlCommand().main(args)
}

@OptIn(ExperimentalTerminalApi::class)
class UnfurlCommand : CliktCommand(name = "unfurl") {
  private val url: String by argument("url")
  private val debug: Boolean by option("-d", "--debug").flag(default = false)

  private val terminal = Terminal(
    // Explicitly enable hyperlinks for mordant to emit OSC 8 escape sequences for
    // clickable hyperlinks in tables. Auto-detection doesn't always work in all terminals.
    hyperlinks = true,
  )
  private val maxWidthOfTableColumn = 52

  override fun run() = runBlocking {
    val url = url.let {
      if (!it.startsWith("http") && !it.startsWith("https")) {
        "https://$it"
      } else {
        it
      }.toHttpUrlOrNull()
    }
    if (url == null) {
      echo("Invalid link", err = true)
      return@runBlocking
    }

    val okHttp = Unfurler.defaultOkHttpClient()
    val unfurler = Unfurler(
      extensions = listOf(MastodonUnfurlerExtension()),
      logger = if (debug) UnfurlLogger.println() else UnfurlLogger.noOp(),
      httpClient = okHttp,
    )
    val unfurled = withProgressAnimation {
      unfurler.unfurl(url)
    }
    if (unfurled == null) {
      echo("Failed to unfurl $url", err = true)
    } else {
      echo()
      printUnfurledLink(unfurled)
      echo()
    }

    okHttp.forceShutDown()
  }

  private suspend fun <T> withProgressAnimation(block: suspend () -> T): T {
    val frames = "⣾⣽⣻⢿⡿⣟⣯⣷"
    val animation = terminal.textAnimation<Int> { frame ->
      green(frames[frame % frames.length].toString())
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

  private fun printUnfurledLink(unfurled: UnfurlResult) {
    terminal.println(
      table {
        body {
          if (url.removeSuffix("/") != unfurled.url.toString().removeSuffix("/")) {
            row("URL", unfurled.url.ellipsizeAndHyperlink())
          }
          row("Title", unfurled.title?.breakLines())
          row("Description", unfurled.description?.breakLines())
          row("Thumbnail", unfurled.thumbnail?.ellipsizeAndHyperlink())
          row("Favicon", unfurled.favicon?.ellipsizeAndHyperlink())

          unfurled.mastodonEngagementStats()?.let { mastodonStats ->
            row("Engagement stats", buildString {
              append(if (mastodonStats.favorites == 1) "1 favorite" else "${mastodonStats.favorites} favorites, ")
              append(if (mastodonStats.replies == 1) "1 reply" else "${mastodonStats.replies} replies, ")
              append(if (mastodonStats.boosts == 1) "1 boost" else "${mastodonStats.boosts} boosts")
            })
          }
        }
      }
    )
  }

  private fun String.breakLines(): String {
    return split("\n")
      .flatMap { it.chunked(maxWidthOfTableColumn) }
      .joinToString(separator = "\n")
  }

  // FYI not all terminals support hyperlinks. At the time
  // of writing this, iTerm does, but macOS terminal does not.
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

private fun OkHttpClient.forceShutDown() {
  // OkHttp uses non-daemon threads which will prevent the JVM from exiting until they time out.
  // https://square.github.io/okhttp/4.x/okhttp/okhttp3/-ok-http-client/#shutdown-isnt-necessary
  dispatcher.executorService.shutdown()
  connectionPool.evictAll()
}
