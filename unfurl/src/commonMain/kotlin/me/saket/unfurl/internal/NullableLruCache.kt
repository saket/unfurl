package me.saket.unfurl.internal

import me.saket.unfurl.internal.NullableLruCache.Optional.None
import me.saket.unfurl.internal.NullableLruCache.Optional.Some

internal class NullableLruCache<K : Any, V>(maxSize: Int) {
  private val delegate = LruCache<K, Optional<V>>(maxSize)

  inline fun computeIfAbsent(key: K, create: () -> V?): V? {
    return when (val cached = delegate[key]) {
      is Some -> cached.value
      is None -> null
      null -> create().also {
        delegate[key] = if (it == null) None else Some(it)
      }
    }
  }

  sealed class Optional<out T> {
    data class Some<out T : Any>(val value: T) : Optional<T>()
    data object None : Optional<Nothing>()
  }
}
