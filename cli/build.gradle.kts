plugins {
  id("application")
  kotlin("jvm")
}

application {
  mainClass.set("me.saket.unfurl.cmd.UnfurlCommandKt")
}

dependencies {
  implementation(project(":unfurl"))
  implementation(project(":unfurl-social"))
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
  implementation("com.github.ajalt.clikt:clikt:3.4.0")
  implementation("com.github.ajalt.mordant:mordant:2.0.0-beta7")

  // The version of OkHttp used by :unfurl fails to download AndroidPolice.com
  // articles with "StreamResetException: stream was reset: PROTOCOL_ERROR" errors.
  // Updating to OkHttp v5.x seems to fix them.
  implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.6")
}
