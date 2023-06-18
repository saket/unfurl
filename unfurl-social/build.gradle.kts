plugins {
  id("java-library")
  kotlin("jvm")
  id("com.google.devtools.ksp").version("1.8.21-1.0.11")
}
apply(plugin = "com.vanniktech.maven.publish")

dependencies {
  implementation(project(":unfurl"))
  implementation("com.squareup.moshi:moshi:1.15.0")
  ksp("com.squareup.moshi:moshi-kotlin-codegen:1.15.0")

  testImplementation("junit:junit:4.13.2")
  testImplementation("com.google.truth:truth:1.1.3")
}
