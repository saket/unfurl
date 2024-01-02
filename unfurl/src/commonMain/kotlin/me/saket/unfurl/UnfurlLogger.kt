package me.saket.unfurl

public interface UnfurlLogger {
  public fun log(message: String)
  public fun log(e: Throwable, message: String)

  public object Println : UnfurlLogger {
    override fun log(e: Throwable, message: String) {
      println(message)
      println(e.stackTraceToString())
    }

    override fun log(message: String) {
      println(message)
    }
  }

  public object NoOp : UnfurlLogger {
    override fun log(message: String): Unit = Unit
    override fun log(e: Throwable, message: String): Unit = Unit
  }
}
