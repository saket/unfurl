plugins {
  id("java-library")
  kotlin("jvm")
}
apply(plugin = "com.vanniktech.maven.publish")

dependencies {
  api("com.squareup.okhttp3:okhttp:4.9.0")  // Updating to 5.x? See cli/build.gradle.kts.
  implementation("org.jsoup:jsoup:1.14.3")
}
