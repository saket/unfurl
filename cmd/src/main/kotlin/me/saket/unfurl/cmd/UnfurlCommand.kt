package me.saket.unfurl.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import kotlinx.coroutines.runBlocking
import me.saket.unfurl.Unfurler
import me.saket.unfurl.social.TweetUnfurler

fun main(args: Array<String>) {
  UnfurlCommand().main(args)
}

class UnfurlCommand : CliktCommand(name = "unfurl") {
  private val url: String by argument("url")
  private val twitterToken: String? by option()

  override fun run(): Unit = runBlocking {
    val unfurler = Unfurler(
      cacheSize = 0,
      delegates = listOfNotNull(
        twitterToken?.let { TweetUnfurler(bearerToken = it) }
      ),
      logger = ::println
    )

    echo("")
    val unfurled = unfurler.unfurl(url)
    if (unfurled == null) {
      echo("Couldn't unfurl", err = true)
    } else {
      with(unfurled) {
        echo("Title: \"${if (title == null) "<empty>" else title}\"")
        echo("Description: \"${if (description == null) "<empty>" else description}\"")
        echo("Thumbnail: $thumbnail")
        echo("Favicon: $favicon")
      }
    }
  }
}
