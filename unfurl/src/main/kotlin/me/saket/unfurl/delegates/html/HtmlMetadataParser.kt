package me.saket.unfurl.delegates.html

import me.saket.unfurl.UnfurlLogger
import me.saket.unfurl.UnfurlResult
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

internal class HtmlMetadataParser(private val logger: UnfurlLogger) {

  fun parse(url: HttpUrl, document: Document): UnfurlResult {
    return UnfurlResult(
      url = url,
      title = parseTitle(document),
      description = parseDescription(document),
      favicon = parseFaviconUrl(document) ?: fallbackFaviconUrl(url),
      thumbnail = parseThumbnailUrl(document)
    )
  }

  private fun parseTitle(document: Document): String? {
    val linkTitle = metaTag(document, "twitter:title", useAbsoluteUrl = false)
      ?: metaTag(document, "og:title", useAbsoluteUrl = false)
      ?: document.title().nullIfBlank()

    if (linkTitle == null) {
      logger.log("couldn't find any title for ${document.baseUri()}.")
    }
    return linkTitle
  }

  private fun parseDescription(document: Document): String? {
    val linkTitle = metaTag(document, "twitter:description", useAbsoluteUrl = false)
      ?: metaTag(document, "og:description", useAbsoluteUrl = false)

    if (linkTitle == null) {
      logger.log("couldn't find any description for ${document.baseUri()}.")
    }
    return linkTitle
  }

  private fun parseThumbnailUrl(document: Document): HttpUrl? {
    // Twitter's image tag is preferred over facebook's
    // because websites seem to give better images for twitter.
    val thumbnailUrl = metaTag(document, "twitter:image", useAbsoluteUrl = true)
      ?: metaTag(document, "og:image", useAbsoluteUrl = true)
      ?: metaTag(document, "twitter:image:src", useAbsoluteUrl = true)
      ?: metaTag(document, "og:image:secure_url", useAbsoluteUrl = true)

    // So... scheme-less URLs are a thing.
    val needsScheme = thumbnailUrl != null && thumbnailUrl.startsWith("//")
    return (if (needsScheme) "https:$thumbnailUrl" else thumbnailUrl)?.toHttpUrlOrNull()
  }

  private fun parseFaviconUrl(document: Document): HttpUrl? {
    val faviconUrl = linkRelTag(document, "apple-touch-icon")
      ?: linkRelTag(document, "apple-touch-icon-precomposed")
      ?: linkRelTag(document, "shortcut icon")
      ?: linkRelTag(document, "icon")
    return faviconUrl?.toHttpUrlOrNull()
  }

  private fun fallbackFaviconUrl(url: HttpUrl): HttpUrl {
    return HttpUrl.Builder()
      .scheme(url.scheme)
      .host(url.host)
      .encodedPath("/favicon.ico")
      .build()
  }

  private fun metaTag(document: Document?, attr: String, useAbsoluteUrl: Boolean): String? {
    val names = document!!.select("meta[name=$attr]")
    val properties = document.select("meta[property=$attr]")

    return sequenceOf(names, properties)
      .flatMap { it }
      .mapNotNull { element: Element ->
        element.attr(if (useAbsoluteUrl) "abs:content" else "content").nullIfBlank()
      }
      .firstOrNull()
  }

  private fun linkRelTag(document: Document, rel: String): String? {
    val elements = document.head().select("link[rel=$rel]")
    var largestSizeUrl = elements.firstOrNull()?.attr("abs:href") ?: return null
    var largestSize = 0

    for (element in elements) {
      // Some websites have multiple icons for different sizes. Find the largest one.
      val sizes = element.attr("sizes")
      if (sizes.contains("x")) {
        val size = sizes.split("x")[0].toInt()
        if (size > largestSize) {
          largestSize = size
          largestSizeUrl = element.attr("abs:href")
        }
      }
    }
    return largestSizeUrl
  }
}

private fun String.nullIfBlank(): String? {
  return ifBlank { null }
}
