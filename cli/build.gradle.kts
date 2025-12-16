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
  implementation(libs.okhttp.coroutines)
  implementation(libs.moshi.core)
  ksp(libs.moshi.codegen)

  testImplementation(libs.junit)
  testImplementation(libs.assertk)
  testImplementation(libs.kotlinx.coroutines.test)
}
