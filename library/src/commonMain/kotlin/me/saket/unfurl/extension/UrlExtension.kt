package me.saket.unfurl.extension

import io.ktor.http.URLBuilder
import io.ktor.http.Url

public fun String.toHttpUrlOrNull(): Url? {
  return try {
    Url(this)
  } catch (_: IllegalArgumentException) {
    null
  }
}

public fun String.toUrl(): Url = URLBuilder(this).build()




