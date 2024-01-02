package me.saket.unfurl.internal

internal typealias Weigher<K, V> = (K, V?) -> Int

/**
 * Multiplatform LRU cache implementation.
 *
 * Implementation is based on usage of [LinkedHashMap] as a container for the cache and custom
 * double linked queue to track LRU property.
 *
 * [maxSize] - maximum size of the cache, can be anything bytes, number of entries etc. By default is number o entries.
 * [weigher] - to be called to calculate the estimated size (weight) of the cache entry defined by its [K] and [V].
 *             By default it returns 1.
 *
 * Cache trim performed only on new entry insertion.
 */
internal class LruCache<K, V>(
  private val maxSize: Int,
  private val weigher: Weigher<K, V> = { _, _ -> 1 }
) {
  private val cache = LinkedHashMap<K, Node<K, V>>(0, 0.75f)
  private var headNode: Node<K, V>? = null
  private var tailNode: Node<K, V>? = null
  private var size: Int = 0

  operator fun get(key: K): V? {
    val node = cache[key]
    if (node != null) {
      moveNodeToHead(node)
    }
    return node?.value
  }

  operator fun set(key: K, value: V) {
    val node = cache[key]
    if (node == null) {
      cache[key] = addNode(key, value)
    } else {
      node.value = value
      moveNodeToHead(node)
    }

    trim()
  }

  fun remove(key: K): V? {
    return removeUnsafe(key)
  }

  fun keys() = cache.keys

  private fun removeUnsafe(key: K): V? {
    val nodeToRemove = cache.remove(key)
    val value = nodeToRemove?.value
    if (nodeToRemove != null) {
      unlinkNode(nodeToRemove)
    }
    return value
  }

  fun remove(keys: Collection<K>) {
    keys.forEach { key -> removeUnsafe(key) }
  }

  fun clear() {
    cache.clear()
    headNode = null
    tailNode = null
    size = 0
  }

  fun size(): Int {
    return size
  }

  fun dump(): Map<K, V> {
    return cache.mapValues { (_, value) -> value.value as V }
  }

  private fun trim() {
    var nodeToRemove = tailNode
    while (nodeToRemove != null && size > maxSize) {
      cache.remove(nodeToRemove.key)
      unlinkNode(nodeToRemove)
      nodeToRemove = tailNode
    }
  }

  private fun addNode(key: K, value: V?): Node<K, V> {
    val node = Node(
      key = key,
      value = value,
      next = headNode,
      prev = null,
    )

    headNode = node

    if (node.next == null) {
      tailNode = headNode
    } else {
      node.next?.prev = headNode
    }

    size += weigher(key, value)

    return node
  }

  private fun moveNodeToHead(node: Node<K, V>) {
    if (node.prev == null) {
      return
    }

    node.prev?.next = node.next
    node.next?.prev = node.prev

    node.next = headNode?.next
    node.prev = null

    headNode?.prev = node
    headNode = node
  }

  private fun unlinkNode(node: Node<K, V>) {
    if (node.prev == null) {
      this.headNode = node.next
    } else {
      node.prev?.next = node.next
    }

    if (node.next == null) {
      this.tailNode = node.prev
    } else {
      node.next?.prev = node.prev
    }

    size -= weigher(node.key!!, node.value)

    node.key = null
    node.value = null
    node.next = null
    node.prev = null
  }

  private class Node<K, V>(
    var key: K?,
    var value: V?,
    var next: Node<K, V>?,
    var prev: Node<K, V>?,
  )
}
