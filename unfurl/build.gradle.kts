plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.dokka)
    id("maven-publish")
}

group = "me.saket.unfurl"
version = libs.versions.libraryVersion.get()

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
kotlin {
    explicitApi()

    jvm()

    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "unfurl"
            isStatic = true
        }
    }

    sourceSets {
        sourceSets {
            commonMain.dependencies {
                implementation(libs.ktor.client.core)
                implementation(libs.ksoup)
            }
            commonTest.dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.ktor.client.mock)
            }

            jvmMain.dependencies {
                implementation(libs.ktor.client.okhttp)
            }

            jvmTest.dependencies {
                implementation(libs.kotlin.test)
            }

            androidMain.dependencies {
                implementation(libs.ktor.client.okhttp)
            }

            iosMain.dependencies {
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
