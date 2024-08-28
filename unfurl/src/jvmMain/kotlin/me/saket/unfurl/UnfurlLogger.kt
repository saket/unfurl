package me.saket.unfurl

interface UnfurlLogger {
  companion object {
    fun println(): UnfurlLogger = PrintlnLogger
    fun noOp(): UnfurlLogger = NoOpLogger
  }

  fun log(message: String)
  fun log(e: Throwable, message: String)
}

private object PrintlnLogger : UnfurlLogger {
  override fun log(e: Throwable, message: String) {
    println(message)
    println(e.stackTraceToString())
  }

  override fun log(message: String) {
    println(message)
  }
}

private object NoOpLogger : UnfurlLogger {
  override fun log(message: String): Unit = Unit
  override fun log(e: Throwable, message: String): Unit = Unit
}
