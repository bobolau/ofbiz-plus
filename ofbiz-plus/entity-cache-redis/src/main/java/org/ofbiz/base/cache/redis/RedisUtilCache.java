package org.ofbiz.base.cache.redis;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericPK;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.model.ModelField;

import redis.clients.jedis.Jedis;

@SuppressWarnings("serial")
public class RedisUtilCache<K, V> extends RedisUtilCacheFactory implements Serializable {

	private static final long serialVersionUID = -6583046019268551550L;

	public static final String module = RedisUtilCache.class.getName();

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

	RedisUtilCache(String cacheName, String... propNames) {
		this.name = cacheName;
		setPropertiesParams(propNames);
	}

	public String getName() {
		return this.name;
	}

	protected String getKeyPrefix() {
		return getName() + "_";
	}

	public void clear() {
		this.redisClearKeyStartwith(getKeyPrefix());
	}

	public V remove(Object key) {
		return (V) redisDel(getRedisKey(key));
	}

	/**
	 * @deprecated this method for ofbiz origin cache
	 */
	public Set<? extends K> getCacheLineKeys() {
		throw new UnsupportedOperationException("getCacheLineKeys in cache by redis");
	}

	public V get(Object key) {
		// EntityCache: GenericValue
		// ConditionCache: Map<Object, List<GenericValue>>, Map<Object, Object>
		return (V) redisGet(getRedisKey(key));
	}

	public V put(K key, V value) {
		// value: GenericValue
		// value: Map<Object, List<GenericValue>>, Map<Object, Object>
		return (V) redisSet(getRedisKey(key), value, expireTimeNanos);
	}

	protected Object redisGet(String key) {
		Jedis jedis = null;
		Boolean error = true;
		try {
			jedis = acquireRedisConnection();
			error = false;
			return unserialize(jedis.get(key.getBytes()));
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
				jedis.pexpire(key, milliseconds);
			}
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
			Object oldValue = unserialize(jedis.get(key.getBytes()));
			jedis.del(key.getBytes());
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
			sb.append(key.toString());
		}
		return sb.toString();
	}

	protected void setPropertiesParams(String cacheName) {
		setPropertiesParams(new String[] { cacheName });
	}

	protected void setPropertiesParams(String[] propNames) {
		ResourceBundle res = getCacheResource();
		if (res != null) {
			String value = getPropertyParam(res, propNames, "expireTime");
			if (UtilValidate.isNotEmpty(value)) {
				this.expireTimeNanos = TimeUnit.NANOSECONDS.convert(Long.parseLong(value), TimeUnit.MILLISECONDS);
			}
		}
	}

}
