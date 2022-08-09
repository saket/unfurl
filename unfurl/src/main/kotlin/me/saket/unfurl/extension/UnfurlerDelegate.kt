package me.saket.unfurl.extension

import me.saket.unfurl.UnfurlLogger
import me.saket.unfurl.UnfurlResult
import okhttp3.HttpUrl
import okhttp3.OkHttpClient

interface UnfurlerExtension {
  fun UnfurlerExtensionScope.unfurl(url: HttpUrl): UnfurlResult?
}

interface UnfurlerExtensionScope {
  val httpClient: OkHttpClient
  val logger: UnfurlLogger
}
