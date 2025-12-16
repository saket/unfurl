package me.saket.unfurl

import dev.drewhamilton.poko.Poko
import me.saket.unfurl.extension.UnfurlerExtension
import okhttp3.HttpUrl
import kotlin.reflect.KClass
import kotlin.reflect.cast

/**
 * @param url The final URL after following any HTTP 3xx redirects.
 * This may differ from the original URL passed to [Unfurler.unfurl].
 * For example, `https://youtu.be/foo` may redirect to
 * `https://www.youtube.com/watch?v=foo`.
 *
 * @param extras Additional values provided by an [UnfurlerExtension].
 * Use [extra()][extra] to read them.
 */
@Poko class UnfurlResult(
  val url: HttpUrl,
  val title: String?,
  val description: String?,
  val favicon: HttpUrl?,
  val thumbnail: HttpUrl?,
  val extras: Map<KClass<*>, Any> = mapOf()
) {

  /**
   * Returns extra information of type [type] provided by a
   * [UnfurlerExtension], or `null` if no such extra is held.
   */
  fun <T : Any> extra(type: KClass<out T>): T? {
    val value = extras[type] ?: return null
    return type.cast(value)
  }
}
