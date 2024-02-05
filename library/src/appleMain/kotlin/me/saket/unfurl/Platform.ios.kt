package me.saket.unfurl

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin

internal actual fun provideHttpClientEngine(): HttpClientEngine {
  return Darwin.create {}
}
