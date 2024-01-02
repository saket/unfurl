package me.saket.unfurl.internal

import io.github.reactivecircus.cache4k.Cache
import me.saket.unfurl.internal.NullableLruCache.Optional.None
import me.saket.unfurl.internal.NullableLruCache.Optional.Some
import kotlin.time.Duration.Companion.hours

internal class NullableLruCache<K : Any, V>(maxSize: Int) {
  private val delegate = Cache.Builder<K, Optional<V>>()
    .maximumCacheSize(maxSize.toLong())
    .expireAfterAccess(24.hours)
    .build()

  inline fun computeIfAbsent(key: K, create: () -> V?): V? {
    return when (val cached = delegate.get(key)) {
      is Some -> cached.value
      is None -> null
      null -> create().also {
        delegate.put(key, if (it == null) None else Some(it))
      }
    }
  }

  sealed class Optional<out T> {
    data class Some<out T : Any>(val value: T) : Optional<T>()
    data object None : Optional<Nothing>()
  }
}
