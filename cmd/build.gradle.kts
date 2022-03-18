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
  implementation("com.github.ajalt.clikt:clikt:3.4.0")
}
