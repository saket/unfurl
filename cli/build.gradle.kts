plugins {
  id("application")
  kotlin("jvm")
}

application {
  mainClass.set("me.saket.unfurl.cmd.UnfurlCommandKt")
}

dependencies {
  implementation(project(":unfurl"))
  implementation("me.saket.unfurl:unfurl-social:1.6.0")
  implementation("com.github.ajalt.clikt:clikt:3.4.0")

  // The version of OkHttp used by :unfurl fails to download AndroidPolice.com
  // articles with "StreamResetException: stream was reset: PROTOCOL_ERROR" errors.
  // Updating to OkHttp v5.x seems to fix them.
  implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.6")
}
