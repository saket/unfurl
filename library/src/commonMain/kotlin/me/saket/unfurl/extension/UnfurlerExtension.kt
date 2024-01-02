package me.saket.unfurl.extension

import io.ktor.client.HttpClient
import io.ktor.http.Url
import me.saket.unfurl.UnfurlLogger
import me.saket.unfurl.UnfurlResult

public interface UnfurlerExtension {
  public suspend fun UnfurlerScope.unfurl(url: Url): UnfurlResult?
}

public interface UnfurlerScope {
  public val httpClient: HttpClient
  public val logger: UnfurlLogger
}
