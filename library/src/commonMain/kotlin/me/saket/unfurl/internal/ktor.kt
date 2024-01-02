package me.saket.unfurl.internal

import io.ktor.http.Url

internal fun String.toUrlOrNull(): Url? {
  return try {
    Url(this)
  } catch (_: IllegalArgumentException) {
    null
  }
}

internal fun String.toUrl(): Url = Url(this)
