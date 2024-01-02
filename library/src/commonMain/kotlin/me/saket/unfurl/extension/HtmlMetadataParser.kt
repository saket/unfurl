package me.saket.unfurl.extension

import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Element
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.appendEncodedPathSegments
import me.saket.unfurl.UnfurlLogger
import me.saket.unfurl.UnfurlResult
import me.saket.unfurl.internal.toUrlOrNull

internal class HtmlMetadataParser(private val logger: UnfurlLogger) {

  fun parse(url: Url, document: Document): UnfurlResult {
    return UnfurlResult(
      url = url,
      title = parseTitle(document),
      description = parseDescription(document),
      favicon = parseFaviconUrl(document) ?: fallbackFaviconUrl(url),
      thumbnail = parseThumbnailUrl(document)
    )
  }

  private fun parseTitle(document: Document): String? {
    val linkTitle = metaTag(document, "twitter:title")
      ?: metaTag(document, "og:title")
      ?: document.title().nullIfBlank()

    if (linkTitle == null) {
      logger.log("couldn't find any title for ${document.baseUri()}.")
    }
    return linkTitle
  }

  private fun parseDescription(document: Document): String? {
    val linkTitle = metaTag(document, "twitter:description")
      ?: metaTag(document, "og:description")
      ?: metaTag(document, "description")

    if (linkTitle == null) {
      logger.log("couldn't find any description for ${document.baseUri()}.")
    }
    return linkTitle
  }

  private fun parseThumbnailUrl(document: Document): Url? {
    // Twitter's image tag is preferred over facebook's
    // because websites seem to give better images for twitter.
    val thumbnailUrl = metaTag(document, "twitter:image", isUrl = true)
      ?: metaTag(document, "og:image", isUrl = true)
      ?: metaTag(document, "twitter:image:src", isUrl = true)
      ?: metaTag(document, "og:image:secure_url", isUrl = true)

    // So... scheme-less URLs are a thing.
    val needsScheme = thumbnailUrl != null && thumbnailUrl.startsWith("//")
    return (if (needsScheme) "https:$thumbnailUrl" else thumbnailUrl)?.toUrlOrNull()
  }

  private fun parseFaviconUrl(document: Document): Url? {
    val faviconUrl = linkRelTag(document, "apple-touch-icon")
      ?: linkRelTag(document, "apple-touch-icon-precomposed")
      ?: linkRelTag(document, "shortcut icon")
      ?: linkRelTag(document, "icon")
    return faviconUrl?.toUrlOrNull()
  }

  private fun fallbackFaviconUrl(url: Url): Url {
    return URLBuilder(protocol = url.protocol, host = url.host)
      .appendEncodedPathSegments("/favicon.ico")
      .build()
  }

  private fun metaTag(document: Document, attr: String, isUrl: Boolean = false): String? {
    val names = document.select("meta[name=$attr]")
    val properties = document.select("meta[property=$attr]")

    return sequenceOf(names, properties)
      .flatMap { it }
      .mapNotNull { element: Element ->
        element.attr(if (isUrl) "abs:content" else "content").nullIfBlank()
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
