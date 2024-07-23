plugins {
  id("java-library")
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.ksp)
  alias(libs.plugins.mavenPublish)
}

dependencies {
  implementation(projects.unfurl)

  implementation(libs.moshi.core)
  ksp(libs.moshi.codegen)

  testImplementation(libs.junit)
  testImplementation(libs.assertk)
}
