plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.dokka)
  alias(libs.plugins.mavenPublish)
  alias(libs.plugins.testResources)
  alias(libs.plugins.poko)
  alias(libs.plugins.metalava)
}

group = "me.saket.unfurl"

kotlin {
  jvm()

  sourceSets {
    commonMain.dependencies {
      api(libs.kotlinx.coroutines.core)
      implementation(libs.cache4k)
    }
    commonTest.dependencies {
      implementation(libs.assertk)
      implementation(libs.testResources)
      implementation(libs.kotlinx.coroutines.test)
    }
    jvmMain.dependencies {
      api(libs.okhttp.core)
      implementation(libs.jsoup)
    }
    jvmTest.dependencies {
      implementation(libs.junit)
      implementation(libs.testParameterInjector)
      implementation(libs.okhttp.mockWebServer)
    }
  }

  jvmToolchain {
    languageVersion.set(JavaLanguageVersion.of(17))
  }
}

metalava {
  filename.set("api/api.txt")
  enforceCheck.set(true)
}
mavenPublishing {
  signAllPublications()
  publishToMavenCentral(automaticRelease = true)

  coordinates("me.saket.unfurl", "unfurl", "2.3.0-SNAPSHOT")
  pom {
    name = "unfurl"
    description = "Generate preview of links, inspired by Slack"
    inceptionYear = "2022"
    url = "https://github.com/saket/unfurl"
    packaging = "jar"
    licenses {
      license {
        name = "The Apache License, Version 2.0"
        url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
        distribution = "http://www.apache.org/licenses/LICENSE-2.0.txt"
      }
    }
    developers {
      developer {
        id = "saket"
        name = "Saket Narayan"
        url = "https://github.com/saket"
      }
    }
    scm {
      url = "https://github.com/saket/unfurl"
      connection = "scm:git@github.com:saket/unfurl.git"
    }
  }
}
