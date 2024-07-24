plugins {
  alias(libs.plugins.kotlin.jvm) apply false
  alias(libs.plugins.dokka) apply false
  alias(libs.plugins.mavenPublish) apply false
  alias(libs.plugins.testResources) apply false
  alias(libs.plugins.ksp) apply false
  alias(libs.plugins.poko) apply false
}
