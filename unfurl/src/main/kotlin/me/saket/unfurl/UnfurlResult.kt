package me.saket.unfurl

import dev.drewhamilton.poko.Poko
import me.saket.unfurl.extension.UnfurlerExtension
import okhttp3.HttpUrl
import kotlin.reflect.KClass
import kotlin.reflect.cast

/**
 * @param url This URL might differ from the original URL used with [Unfurler.unfurl],
 * if HTTP 3xx redirects were followed. For instance, `https://youtu.be/foo` might
 * redirect to `https://www.youtube.com/watch?v=foo`.
 *
 * @param extras Additional values that can be populated by an [UnfurlerExtension].
 * Use [extra] for reading them.
 */
@Poko class UnfurlResult(
  val url: HttpUrl,
  val title: String?,
  val description: String?,
  val favicon: HttpUrl?,
  val thumbnail: HttpUrl?,
  val extras: Map<KClass<*>, Any> = mapOf()
) {

  /** Returns extra metadata of type [type], or null if no such metadata is held. */
  fun <T : Any> extra(type: KClass<out T>): T? {
    val value = extras[type] ?: return null
    return type.cast(value)
  }
}
