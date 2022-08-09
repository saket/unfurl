plugins {
  id("java-library")
  kotlin("jvm")
}
apply(plugin = "com.vanniktech.maven.publish")

dependencies {
  api("com.squareup.okhttp3:okhttp:4.9.0")  // Updating to 5.x? See cli/build.gradle.kts.
  api("org.jsoup:jsoup:1.14.3")

  testImplementation("junit:junit:4.13.2")
  testImplementation("com.google.truth:truth:1.1.3")
  testImplementation("com.google.testparameterinjector:test-parameter-injector:1.8")
  testImplementation("com.squareup.okhttp3:mockwebserver:4.10.0")
}
