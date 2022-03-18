plugins {
  id("java-library")
  kotlin("jvm")
}
apply(plugin = "com.vanniktech.maven.publish")

dependencies {
  api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
  api("com.squareup.okhttp3:okhttp:4.9.0")
  implementation("org.jsoup:jsoup:1.14.3")
}
