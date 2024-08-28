package me.saket.unfurl.extension

import me.saket.unfurl.UnfurlLogger
import me.saket.unfurl.UnfurlResult
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import com.fleeksoft.ksoup.nodes.Document as KsoupDocument

internal class HtmlMetadataParser(private val logger: UnfurlLogger) {

  fun parse(url: HttpUrl, document: KsoupDocument): UnfurlResult {
    return UnfurlResult(
      url = url,
      title = parseTitle(document),
      description = parseDescription(document),
      favicon = parseFaviconUrl(document) ?: fallbackFaviconUrl(url),
      thumbnail = parseThumbnailUrl(document)
    )
  }

  private fun parseTitle(document: KsoupDocument): String? {
    val linkTitle = metaTag(document, "twitter:title")
      ?: metaTag(document, "og:title")
      ?: document.title().nullIfBlank()

    if (linkTitle == null) {
      logger.log("couldn't find any title for ${document.baseUri()}.")
    }
    return linkTitle
  }

  private fun parseDescription(document: KsoupDocument): String? {
    val linkTitle = metaTag(document, "twitter:description")
      ?: metaTag(document, "og:description")
      ?: metaTag(document, "description")

    if (linkTitle == null) {
      logger.log("couldn't find any description for ${document.baseUri()}.")
    }
    return linkTitle
  }

  private fun parseThumbnailUrl(document: KsoupDocument): HttpUrl? {
    // Twitter's image tag is preferred over facebook's
    // because websites seem to give better images for twitter.
    val thumbnailUrl = metaTag(document, "twitter:image", isUrl = true)
      ?: metaTag(document, "og:image", isUrl = true)
      ?: metaTag(document, "twitter:image:src", isUrl = true)
      ?: metaTag(document, "og:image:secure_url", isUrl = true)

    // So... scheme-less URLs are a thing.
    val needsScheme = thumbnailUrl != null && thumbnailUrl.startsWith("//")
    return (if (needsScheme) "https:$thumbnailUrl" else thumbnailUrl)?.toHttpUrlOrNull()
  }

  private fun parseFaviconUrl(document: KsoupDocument): HttpUrl? {
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

  private fun metaTag(document: KsoupDocument, attr: String, isUrl: Boolean = false): String? {
    return listOf(
      document.select("meta[name=$attr]"),
      document.select("meta[property=$attr]"),
    ).firstNotNullOfOrNull {
      it.attr(if (isUrl) "abs:content" else "content").nullIfBlank()
    }
  }

  private fun linkRelTag(document: KsoupDocument, rel: String): String? {
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
