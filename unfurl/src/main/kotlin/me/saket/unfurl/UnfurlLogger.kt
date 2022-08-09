package me.saket.unfurl

interface UnfurlLogger {
  fun log(message: String)
  fun log(e: Throwable, message: String)

  companion object

  object Println : UnfurlLogger {
    override fun log(e: Throwable, message: String) {
      println(message)
      println(e.stackTraceToString())
    }

    override fun log(message: String) {
      println(message)
    }
  }

  object NoOp : UnfurlLogger {
    override fun log(message: String) = Unit
    override fun log(e: Throwable, message: String) = Unit
  }
}
