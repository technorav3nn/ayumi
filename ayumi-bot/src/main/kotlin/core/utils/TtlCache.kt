package dev.deathblows.app.core.utils

import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class TtlCache<K, V>(private val defaultTtl: Duration = 10.minutes) {
	private class CacheEntry<V>(val value: V, expirationTimeMillis: Long) {
		val expiryTime: Long = expirationTimeMillis
		fun isExpired(): Boolean = System.currentTimeMillis() > expiryTime
	}

	private val cache = ConcurrentHashMap<K, CacheEntry<V>>()

	fun set(key: K, value: V, ttl: Duration = defaultTtl) {
		val expirationTime = System.currentTimeMillis() + ttl.inWholeMilliseconds
		cache[key] = CacheEntry(value, expirationTime)
	}

	fun get(key: K): V? {
		val entry = cache[key] ?: return null

		return if (entry.isExpired()) {
			cache.remove(key)
			null
		} else {
			entry.value
		}
	}
}
