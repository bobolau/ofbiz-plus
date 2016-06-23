package org.ofbiz.base.cache.redis;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RedisUtilCacheFactory {

	/** A static Map to keep track of all of the UtilCache instances. */
	private static final ConcurrentHashMap<String, RedisUtilCache<?, ?>> utilCacheTable = new ConcurrentHashMap<String, RedisUtilCache<?, ?>>();

	private static RedisUtilCacheFactory __factory = new RedisUtilCacheFactory();

	public static RedisUtilCacheFactory getInstance() {
		return __factory;
	}

	RedisUtilCacheFactory() {
	}

	@SuppressWarnings("unchecked")
	public static <K, V> RedisUtilCache<K, V> getOrCreateUtilCache(String cacheName, String... propNames) {
		RedisUtilCache<K, V> existingCache = (RedisUtilCache<K, V>) utilCacheTable.get(cacheName);
		if (existingCache != null)
			return existingCache;
		RedisUtilCache<K, V> newCache = new RedisUtilCache<K, V>(cacheName, propNames);
		utilCacheTable.putIfAbsent(cacheName, newCache);
		return (RedisUtilCache<K, V>) utilCacheTable.get(cacheName);
	}

	@SuppressWarnings("unchecked")
	public static <K, V> RedisUtilCache<K, V> findCache(String cacheName) {
		return (RedisUtilCache<K, V>) utilCacheTable.get(cacheName);
	}

	public static void clearCache(String cacheName) {
		RedisUtilCache<?, ?> cache = findCache(cacheName);
		if (cache == null)
			return;
		cache.clear();

	}

	public static void clearCachesThatStartWith(String startsWith) {
		for (Map.Entry<String, RedisUtilCache<?, ?>> entry : utilCacheTable.entrySet()) {
			String name = entry.getKey();
			if (name.startsWith(startsWith)) {
				RedisUtilCache<?, ?> cache = entry.getValue();
				cache.clear();
			}
		}

	}

}
