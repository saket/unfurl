package me.saket.unfurl

import okhttp3.HttpUrl

data class UnfurlResult(
  val url: HttpUrl,
  val title: String?,
  val description: String?,
  val favicon: HttpUrl?,
  val thumbnail: HttpUrl?,
)
