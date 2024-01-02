package me.saket.unfurl

import io.ktor.client.engine.HttpClientEngine

internal expect fun provideHttpClientEngine(): HttpClientEngine
