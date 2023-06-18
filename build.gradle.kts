buildscript {
  repositories {
    mavenCentral()
    gradlePluginPortal()
  }

  dependencies {
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.21") // Don't forget to update KSP.
    classpath("com.vanniktech:gradle-maven-publish-plugin:0.24.0")
    classpath("org.jetbrains.dokka:dokka-gradle-plugin:1.8.10")
  }
}

allprojects {
  repositories {
    mavenCentral()
  }
}
