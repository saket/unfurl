package me.saket.unfurl.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import kotlinx.coroutines.runBlocking
import me.saket.unfurl.Unfurler

fun main(args: Array<String>) {
  UnfurlCommand().main(args)
}

class UnfurlCommand : CliktCommand(name = "unfurl") {
  private val url: String by argument("url")

  override fun run(): Unit = runBlocking {
    val unfurler = Unfurler(
      cacheSize = 0,
      logger = ::println
    )

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
