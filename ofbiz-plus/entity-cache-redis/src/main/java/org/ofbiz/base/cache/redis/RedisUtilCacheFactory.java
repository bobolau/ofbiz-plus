package org.ofbiz.base.cache.redis;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

import org.ofbiz.base.util.Debug;

import redis.clients.jedis.Jedis;

public class RedisUtilCacheFactory {

	/** A static Map to keep track of all of the UtilCache instances. */
	private static final ConcurrentHashMap<String, RedisUtilCache<?, ?>> utilCacheTable = new ConcurrentHashMap<String, RedisUtilCache<?, ?>>();

	private RedisCacheManager redisCacheManager = null;

	RedisUtilCacheFactory() {
	}

	private Map utilCacheTable() {
		return this.utilCacheTable;
	}

	private void init() {
		if (redisCacheManager == null) {
			initRedis();
		}
	}

	private synchronized void initRedis() {
		if (redisCacheManager == null) {
			redisCacheManager = new RedisCacheManager();
			String[] propNames = new String[] { "entitycache" };
			ResourceBundle res = getCacheResource();
			if (res != null) {
				String redishost = getPropertyParam(res, propNames, "redis-host", null);
				if (redishost != null) {
					redisCacheManager.setHost(redishost);
				}
				int redisPort = getPropertyParam(res, propNames, "redis-port", 0);
				if (redisPort > 0) {
					redisCacheManager.setPort(redisPort);
				}
				int redisDatabase = getPropertyParam(res, propNames, "redis-database", 0);
				if (redisDatabase > 0) {
					redisCacheManager.setDatabase(redisDatabase);
				}
				String redisPassword = getPropertyParam(res, propNames, "redis-password", null);
				if (redisPassword != null) {
					redisCacheManager.setPassword(redisPassword);
				}
				int redisTimeout = getPropertyParam(res, propNames, "redis-timeout", -1);
				if (redisTimeout >= 0) {
					redisCacheManager.setTimeout(redisTimeout);
				}
				String sentinelMaster = getPropertyParam(res, propNames, "redis-sentinelMaster", null);
				if (sentinelMaster != null) {
					redisCacheManager.setSentinelMaster(sentinelMaster);
				}
			}
			redisCacheManager.initializeDatabaseConnection();
		}
	}

	protected Jedis acquireRedisConnection() {
		init();
		return redisCacheManager.acquireConnection();
	}

	protected void returnRedisConnection(Jedis jedis, Boolean error) {
		init();
		redisCacheManager.returnConnection(jedis, error);
	}

	protected void returnRedisConnection(Jedis jedis) {
		returnRedisConnection(jedis, false);
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

	/////////////////////////////////////////////////////////////////////////////////
	protected ResourceBundle getCacheResource() {
		ResourceBundle res = ResourceBundle.getBundle("cache");
		return res;
	}

	protected String getPropertyParam(ResourceBundle res, String[] propNames, String parameter, String defaultString) {
		String value = getPropertyParam(res, propNames, parameter);
		if (value == null) {
			value = defaultString;
		}
		return value;
	}

	protected int getPropertyParam(ResourceBundle res, String[] propNames, String parameter, int defaultInt) {
		int intValue = defaultInt;
		String strvalue = getPropertyParam(res, propNames, parameter);
		try {
			intValue = Integer.parseInt(strvalue);
		} catch (Throwable t) {
		}
		return intValue;
	}

	protected String getPropertyParam(ResourceBundle res, String[] propNames, String parameter) {
		try {
			for (String propName : propNames) {
				if (res.containsKey(propName + '.' + parameter)) {
					try {
						return res.getString(propName + '.' + parameter);
					} catch (MissingResourceException e) {
					}
				}
			}
			// don't need this, just return null
			// if (value == null) {
			// throw new MissingResourceException("Can't find resource for
			// bundle", res.getClass().getName(), Arrays.asList(propNames) + "."
			// + parameter);
			// }
		} catch (Exception e) {
			Debug.logWarning(e,
					"Error getting " + parameter + " value from cache.properties file for propNames: " + propNames,
					"catalina");
		}
		return null;
	}

	///////////////////////////////////////////////////////////////////////////////////////////
	protected static byte[] serialize(Object object) {
		ObjectOutputStream oos = null;
		ByteArrayOutputStream baos = null;
		try {
			// 序列化
			baos = new ByteArrayOutputStream();
			oos = new ObjectOutputStream(baos);
			oos.writeObject(object);
			byte[] bytes = baos.toByteArray();
			return bytes;
		} catch (Exception e) {

		}
		return null;
	}

	protected static Object unserialize(byte[] bytes) {
		ByteArrayInputStream bais = null;
		try {
			// 反序列化
			bais = new ByteArrayInputStream(bytes);
			ObjectInputStream ois = new ObjectInputStream(bais);
			return ois.readObject();
		} catch (Exception e) {

		}
		return null;
	}

}
