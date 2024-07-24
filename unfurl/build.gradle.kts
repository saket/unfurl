plugins {
  id("java-library")
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.dokka)
  alias(libs.plugins.mavenPublish)
  alias(libs.plugins.testResources)
  alias(libs.plugins.poko)
}

group = "me.saket.unfurl"

kotlin {
  jvmToolchain {
    languageVersion.set(JavaLanguageVersion.of(17))
  }
}

dependencies {
  api(libs.kotlinx.coroutines.core)
  api(libs.okhttp.core)

  implementation(libs.jsoup)
  implementation(libs.aedile)

  testImplementation(libs.junit)
  testImplementation(libs.assertk)
  testImplementation(libs.testResources)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.testParameterInjector)
  testImplementation(libs.okhttp.mockWebServer)
}
