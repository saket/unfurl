package me.saket.unfurl.delegates.html

import me.saket.unfurl.UnfurlResult
import me.saket.unfurl.delegates.UnfurlerDelegateScope
import okhttp3.HttpUrl
import org.jsoup.nodes.Document

interface HtmlMetadataParser {
  fun UnfurlerDelegateScope.parse(url: HttpUrl, document: Document): UnfurlResult?
}
