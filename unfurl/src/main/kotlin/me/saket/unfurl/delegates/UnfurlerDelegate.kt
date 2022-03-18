package me.saket.unfurl.delegates

import me.saket.unfurl.Logger
import me.saket.unfurl.UnfurlResult
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import kotlin.coroutines.CoroutineContext

interface UnfurlerDelegate {
  fun UnfurlerDelegateScope.unfurl(url: HttpUrl): UnfurlResult?
}

interface UnfurlerDelegateScope {
  val dispatcher: CoroutineContext
  val httpClient: OkHttpClient
  val logger: Logger
}
