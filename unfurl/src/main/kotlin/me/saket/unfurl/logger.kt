package me.saket.unfurl

fun interface Logger {
  fun log(message: String)
}

internal object PrintlnLogger : Logger {
  override fun log(message: String) {
    println(message)
  }
}
