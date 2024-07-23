package me.saket.unfurl.internal

import com.sksamuel.aedile.core.cacheBuilder
import me.saket.unfurl.internal.NullableLruCache.Optional.None
import me.saket.unfurl.internal.NullableLruCache.Optional.Some
import kotlin.time.Duration.Companion.hours

internal class NullableLruCache<K : Any, V>(maxSize: Int) {
  private val delegate = cacheBuilder<K, Optional<V>> {
    useCallingContext = true
    expireAfterAccess = 24.hours
    maximumSize = maxSize.toLong()
  }.build()

  suspend inline fun computeIfAbsent(key: K, create: () -> V?): V? {
    return when (val cached = delegate.getIfPresent(key)) {
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
