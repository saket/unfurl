plugins {
  alias(libs.plugins.androidLibrary)
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.dokka)
  alias(libs.plugins.mavenPublish)
  alias(libs.plugins.testResources)
}

group = "me.saket.unfurl"
version = libs.versions.libraryVersion.get()

kotlin {
  explicitApi()

  applyDefaultHierarchyTemplate()
  jvm()
  androidTarget()
  iosX64()
  iosArm64()
  iosSimulatorArm64()
  linuxX64()
  linuxArm64()
  macosX64()
  macosArm64()
  mingwX64()

  sourceSets {
    sourceSets {
      commonMain.dependencies {
        api(libs.kotlinx.coroutines.core)
        implementation(libs.ktor.client.core)
        implementation(libs.ksoup)
        implementation(libs.cache4k)
      }
      commonTest.dependencies {
        implementation(libs.ktor.client.mock)
        implementation(libs.kotlin.test)
        implementation(libs.assertk)
        implementation(libs.testResources)
        implementation(libs.kotlinx.coroutines.test)
      }
      jvmMain.dependencies {
        implementation(libs.ktor.client.okhttp)
      }
      androidMain.dependencies {
        implementation(libs.ktor.client.okhttp)
      }
      appleMain.dependencies {
        implementation(libs.ktor.client.darwin)
      }
    }
  }
}

android {
  namespace = "me.saket.unfurl"
  compileSdk = libs.versions.compileSdk.get().toInt()
  defaultConfig {
    minSdk = libs.versions.minSdk.get().toInt()
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
}
