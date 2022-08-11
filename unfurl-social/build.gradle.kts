plugins {
  id("java-library")
  kotlin("jvm")
  id("com.google.devtools.ksp").version("1.7.10-1.0.6")
}
apply(plugin = "com.vanniktech.maven.publish")

dependencies {
  implementation(project(":unfurl"))
  implementation("com.squareup.moshi:moshi:1.13.0")
  ksp("com.squareup.moshi:moshi-kotlin-codegen:1.13.0")

  testImplementation("junit:junit:4.13.2")
  testImplementation("com.google.truth:truth:1.1.3")
}
