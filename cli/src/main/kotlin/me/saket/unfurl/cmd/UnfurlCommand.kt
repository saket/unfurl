package me.saket.unfurl.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import me.saket.unfurl.Unfurler
import me.saket.unfurl.social.TweetUnfurler
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

fun main(args: Array<String>) {
  UnfurlCommand().main(args)
}

class UnfurlCommand : CliktCommand(name = "unfurl") {
  private val url: String by argument("url")
  private val twitterToken: String? by option()
  private val debug: Boolean by option().flag(default = false)

  override fun run() {
    if (url.toHttpUrlOrNull() == null) {
      echo("Invalid link", err = true)
      return
    }

    val unfurler = Unfurler(
      cacheSize = 0,
      delegates = listOfNotNull(
        twitterToken?.let { TweetUnfurler(bearerToken = it) }
      ),
      logger = {
        if (debug) {
          println(it)
        }
      }
    )

    val unfurled = unfurler.unfurl(url)
    if (unfurled == null) {
      echo("Couldn't unfurl", err = true)
    } else {
      echo("")
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
}
