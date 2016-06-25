package org.ofbiz.base.cache.redis;

import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilObject;

import redis.clients.jedis.Jedis;

public class UtilRedisCacheFactory {

	/** A static Map to keep track of all of the UtilCache instances. */
	private static final ConcurrentHashMap<String, UtilRedisCache<?, ?>> utilCacheTable = new ConcurrentHashMap<String, UtilRedisCache<?, ?>>();

	/**
	 * An index number appended to utilCacheTable names when there are
	 * conflicts.
	 */
	private final static ConcurrentHashMap<String, AtomicInteger> defaultIndices = new ConcurrentHashMap<String, AtomicInteger>();

	private static RedisManager redisManager = null;

	private UtilRedisCacheFactory() {
	}

	private static RedisManager getRedisManager() {
		if (redisManager == null) {
			initRedis();
		}
		return redisManager;
	}

	private synchronized static void initRedis() {
		if (redisManager == null) {
			redisManager = new RedisManager();
			String[] propNames = new String[] { "entitycache" };
			ResourceBundle res = getCacheResource();
			if (res != null) {
				String redishost = getPropertyParam(res, propNames, "redis-host", null);
				if (redishost != null) {
					redisManager.setHost(redishost);
				}
				int redisPort = getPropertyParam(res, propNames, "redis-port", 0);
				if (redisPort > 0) {
					redisManager.setPort(redisPort);
				}
				int redisDatabase = getPropertyParam(res, propNames, "redis-database", 0);
				if (redisDatabase > 0) {
					redisManager.setDatabase(redisDatabase);
				}
				String redisPassword = getPropertyParam(res, propNames, "redis-password", null);
				if (redisPassword != null && !"".equals(redisPassword.trim())) {
					redisManager.setPassword(redisPassword);
				}
				int redisTimeout = getPropertyParam(res, propNames, "redis-timeout", -1);
				if (redisTimeout >= 0) {
					redisManager.setTimeout(redisTimeout);
				}
				String sentinelMaster = getPropertyParam(res, propNames, "redis-sentinelMaster", null);
				if (sentinelMaster != null && !"".equals(sentinelMaster.trim())) {
					redisManager.setSentinelMaster(sentinelMaster);
				}
			}
			redisManager.initializeDatabaseConnection();
		}
	}

	@SuppressWarnings("unchecked")
	public static <K, V> UtilRedisCache<K, V> getOrCreateUtilCache(String name, String... propNames) {
		UtilRedisCache<K, V> existingCache = (UtilRedisCache<K, V>) utilCacheTable.get(name);
		if (existingCache != null)
			return existingCache;
		UtilRedisCache<K, V> newCache = new UtilRedisCache<K, V>(name + getNextDefaultIndex(name), propNames);
		newCache.setRedisManager(getRedisManager());
		utilCacheTable.putIfAbsent(name, newCache);
		return (UtilRedisCache<K, V>) utilCacheTable.get(name);
	}

	@SuppressWarnings("unchecked")
	public static <K, V> UtilRedisCache<K, V> findCache(String name) {
		// UtilCache
		return (UtilRedisCache<K, V>) utilCacheTable.get(name);
	}

	public static void clearCache(String name) {
		UtilRedisCache<?, ?> cache = findCache(name);
		if (cache == null)
			return;
		cache.clear();

	}

	public static void clearCachesThatStartWith(String startsWith) {
		for (Map.Entry<String, UtilRedisCache<?, ?>> entry : utilCacheTable.entrySet()) {
			String name = entry.getKey();
			if (name.startsWith(startsWith)) {
				UtilRedisCache<?, ?> cache = entry.getValue();
				cache.clear();
			}
		}

	}

	/////////////////////////////////////////////////////////////////////////////////

	private static String getNextDefaultIndex(String cacheName) {
		AtomicInteger curInd = defaultIndices.get(cacheName);
		if (curInd == null) {
			defaultIndices.putIfAbsent(cacheName, new AtomicInteger(0));
			curInd = defaultIndices.get(cacheName);
		}
		int i = curInd.getAndIncrement();
		return i == 0 ? "" : Integer.toString(i);
	}

	protected static ResourceBundle getCacheResource() {
		ResourceBundle res = ResourceBundle.getBundle("entitycache");
		if (res == null) {
			res = ResourceBundle.getBundle("cache");
		}
		return res;
	}

	protected static String getPropertyParam(ResourceBundle res, String[] propNames, String parameter,
			String defaultString) {
		String value = getPropertyParam(res, propNames, parameter);
		if (value == null) {
			value = defaultString;
		}
		return value;
	}

	protected static int getPropertyParam(ResourceBundle res, String[] propNames, String parameter, int defaultInt) {
		int intValue = defaultInt;
		String strvalue = getPropertyParam(res, propNames, parameter);
		try {
			intValue = Integer.parseInt(strvalue);
		} catch (Throwable t) {
		}
		return intValue;
	}

	protected static String getPropertyParam(ResourceBundle res, String[] propNames, String parameter) {
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

	

}
