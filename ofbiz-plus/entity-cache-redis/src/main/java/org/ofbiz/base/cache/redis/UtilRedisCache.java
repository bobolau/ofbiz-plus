package org.ofbiz.base.cache.redis;

import java.io.Serializable;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilObject;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericPK;
import org.ofbiz.entity.condition.EntityCondition;

import redis.clients.jedis.Jedis;

/**
 * 1-entity:(entityname+pk)->entity/view
 * 2-entityList:(entityname+conditionKey)->map(orderBy->List)
 * 3-entityObject:(entityname+conditionKey)->map(filedname->object)
 */

@SuppressWarnings("serial")
public class UtilRedisCache<K, V>
		// extends UtilRedisCacheFactory
		implements Serializable {

	private static final long serialVersionUID = -6583046019268551550L;

	public static final String module = UtilRedisCache.class.getName();

	private RedisManager redisManager = null;

	/**
	 * The name of the UtilCache instance, is also the key for the instance in
	 * utilCacheTable.
	 */
	private final String name;

	/**
	 * Specifies the amount of time since initial loading before an element will
	 * be reported as expired. If set to 0, elements will never expire.
	 */
	protected long expireTimeNanos = 0;

	UtilRedisCache(String cacheName, String... propNames) {
		super();
		this.name = cacheName;
		setPropertiesParams(propNames);
	}

	public String getName() {
		return this.name;
	}

	void setRedisManager(RedisManager redisManager) {
		this.redisManager = redisManager;
	}

	protected Jedis acquireRedisConnection() {
		return redisManager.acquireConnection();
	}

	protected void returnRedisConnection(Jedis jedis, Boolean error) {
		redisManager.returnConnection(jedis, error);
	}

	protected void returnRedisConnection(Jedis jedis) {
		returnRedisConnection(jedis, false);
	}

	protected String getKeyPrefix() {
		return getName() + "_";
	}

	public void clear() {
		this.redisClearKeyStartwith(getKeyPrefix());
	}

	public void clear(Object conditionKey) {
		redisClearKeyStartwith(getRedisKey(conditionKey));
	}

	public V remove(Object key) {
		return (V) redisDel(getRedisKey(key));
	}

	public V remove(Object conditionKey, Object key) {
		return (V) redisDel(getRedisKey(conditionKey, key));
	}

	public V get(Object key) {
		V value = (V) redisGet(getRedisKey(key));
		return value;
	}

	public V get(Object conditionKey, Object key) {
		V value = (V) redisGet(getRedisKey(conditionKey, key));
		return value;
	}

	public V put(K key, V value) {
		return (V) redisSet(getRedisKey(key), value, expireTimeNanos);
	}

	public V put(Object conditionKey, K key, V value) {
		return (V) redisSet(getRedisKey(conditionKey, key), value, expireTimeNanos);
	}

	/**
	 * @deprecated this method for ofbiz origin cache
	 */
	public Set<? extends K> getCacheLineKeys() {
		throw new UnsupportedOperationException("getCacheLineKeys in cache by redis");
	}

	public Set<String> redisKeyset(String startwith) {

		Jedis jedis = null;
		Boolean error = true;
		try {
			jedis = acquireRedisConnection();
			Set<String> keySet = jedis.keys(startwith + "*");
			error = false;
			return keySet;
		} finally {
			if (jedis != null) {
				returnRedisConnection(jedis, error);
			}
		}
	}

	protected Object redisGet(String key) {
		Jedis jedis = null;
		Boolean error = true;
		try {
			jedis = acquireRedisConnection();
			error = false;
			Object value = deserialize(jedis.get(key.getBytes()));
			if (Debug.verboseOn())
				Debug.logVerbose("redis get with key [" + key + "], result is [" + value + "]", "redis");
			return value;
		} finally {
			if (jedis != null) {
				returnRedisConnection(jedis, error);
			}
		}
	}

	protected Object redisSet(String key, Object value, long milliseconds) {
		Jedis jedis = null;
		Boolean error = true;
		try {
			jedis = acquireRedisConnection();
			error = false;
			String obj = jedis.set(key.getBytes(), serialize(value));
			if (milliseconds > 0) {
				jedis.pexpire(key.getBytes(), milliseconds);
			}
			if (Debug.verboseOn())
				Debug.logVerbose("redis set with key [" + key + "], value is [" + value + "]"
						+ (milliseconds > 0 ? ", expire [" + milliseconds + "] millisends" : ""), "redis");
			return value;
		} finally {
			if (jedis != null) {
				returnRedisConnection(jedis, error);
			}
		}
	}

	protected void redisClearKeyStartwith(String key) {
		Jedis jedis = null;
		Boolean error = true;
		try {
			jedis = acquireRedisConnection();
			Set<String> keySet = jedis.keys(key + "*");
			error = false;
			if (Debug.verboseOn())
				Debug.logVerbose("redis clear start with key [" + key + "]", "redis");
			for (String kk : keySet) {
				jedis.del(kk);
			}
		} finally {
			if (jedis != null) {
				returnRedisConnection(jedis, error);
			}
		}

	}

	protected Object redisDel(String key) {
		Jedis jedis = null;
		Boolean error = true;
		try {
			jedis = acquireRedisConnection();
			error = false;
			Object oldValue = deserialize(jedis.get(key.getBytes()));
			jedis.del(key.getBytes());
			if (Debug.verboseOn())
				Debug.logVerbose("redis del with key [" + key + "]", "redis");
			return oldValue;
		} finally {
			if (jedis != null) {
				returnRedisConnection(jedis, error);
			}
		}
	}

	protected void redisClear() {
		Jedis jedis = null;
		Boolean error = true;
		try {
			jedis = acquireRedisConnection();
			if (Debug.verboseOn())
				Debug.logVerbose("redis clear ...", "redis");
			jedis.flushDB();
			error = false;
		} finally {
			if (jedis != null) {
				returnRedisConnection(jedis, error);
			}
		}
	}

	////////////////////////////////////////////////////////////////////

	protected String getRedisKey(Object key) {
		StringBuffer sb = new StringBuffer();
		sb.append(getKeyPrefix());
		if (key instanceof GenericPK) { // PK
			sb.append(((GenericPK) key).getPkShortValueString());
		} else if (key instanceof EntityCondition) { // conditionKey
			sb.append(((EntityCondition) key).toString()); // where
		} else {
			sb.append(key);
		}
		return sb.toString();
	}

	protected String getRedisKey(Object conditionKey, Object key) {
		StringBuffer sb = new StringBuffer();
		sb.append(getKeyPrefix());
		sb.append(conditionKey);
		sb.append("_").append(key);
		return sb.toString();
	}

	protected void setPropertiesParams(String cacheName) {
		setPropertiesParams(new String[] { cacheName });
	}

	protected void setPropertiesParams(String[] propNames) {
		ResourceBundle res = UtilRedisCacheFactory.getCacheResource();
		if (res != null) {
			String value = UtilRedisCacheFactory.getPropertyParam(res, propNames, "expireTime");
			if (UtilValidate.isNotEmpty(value)) {
				this.expireTimeNanos = TimeUnit.NANOSECONDS.convert(Long.parseLong(value), TimeUnit.MILLISECONDS);
			}
		}
	}

	///////////////////////////////////////////////////////////////////////////////////////////
	protected static byte[] serialize(Object object) {
		if (object == null)
			return null;
		return UtilObject.getBytes(object);
	}

	protected static Object deserialize(byte[] bytes) {
		if (bytes == null)
			return null;
		return UtilObject.getObject(bytes);
	}

}
