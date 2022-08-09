package me.saket.unfurl.internal

class LruCache<K, V>(
  private val cacheSize: Int
) : LinkedHashMap<K, V>(cacheSize, /* loadFactor = */0.75f, /* accessOrder = */ true) {

  init {
    require(cacheSize >= 0)
  }

  override fun removeEldestEntry(eldest: Map.Entry<K, V>): Boolean {
    return size > cacheSize
  }
}
