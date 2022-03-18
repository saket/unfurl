package me.saket.unfurl.delegates.html

import me.saket.unfurl.UnfurlResult
import okhttp3.HttpUrl
import org.jsoup.nodes.Document

interface HtmlMetadataParser {
  fun parse(url: HttpUrl, document: Document): UnfurlResult?
}
