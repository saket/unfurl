buildscript {
  repositories {
    mavenCentral()
    gradlePluginPortal()
  }

  dependencies {
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.10")
    classpath("com.vanniktech:gradle-maven-publish-plugin:0.19.0")
    classpath("org.jetbrains.dokka:dokka-gradle-plugin:1.6.10")
  }
}

allprojects {
  repositories {
    mavenCentral()
  }
}
