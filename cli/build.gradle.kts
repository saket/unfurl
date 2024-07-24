plugins {
  id("application")
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.ksp)
}

application {
  mainClass.set("me.saket.unfurl.cmd.UnfurlCommandKt")
}

dependencies {
  implementation(projects.unfurl)

  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.clikt)
  implementation(libs.mordant)
  implementation(libs.moshi.core)
  ksp(libs.moshi.codegen)

  // The version of OkHttp used by :unfurl fails to download AndroidPolice.com
  // articles with "StreamResetException: stream was reset: PROTOCOL_ERROR" errors.
  // Updating to OkHttp v5.x seems to fix them.
  implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.11")

  testImplementation(libs.junit)
  testImplementation(libs.assertk)
  testImplementation(libs.kotlinx.coroutines.test)
}
