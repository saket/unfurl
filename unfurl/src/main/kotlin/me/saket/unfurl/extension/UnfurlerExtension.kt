package me.saket.unfurl.extension

import me.saket.unfurl.UnfurlLogger
import me.saket.unfurl.UnfurlResult
import okhttp3.HttpUrl
import okhttp3.OkHttpClient

interface UnfurlerExtension {
  fun UnfurlerScope.unfurl(url: HttpUrl): UnfurlResult?
}

interface UnfurlerScope {
  val httpClient: OkHttpClient
  val logger: UnfurlLogger
}
