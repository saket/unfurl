package me.saket.unfurl.delegates

import me.saket.unfurl.UnfurlLogger
import me.saket.unfurl.UnfurlResult
import okhttp3.HttpUrl
import okhttp3.OkHttpClient

interface UnfurlerDelegate {
  fun UnfurlerDelegateScope.unfurl(url: HttpUrl): UnfurlResult?
}

interface UnfurlerDelegateScope {
  val httpClient: OkHttpClient
  val logger: UnfurlLogger
}
