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
 * 1-entity:(entityname)->map(pk->entity/view)
 * 2-entityList:(entityname)->map(conditionKey+orderBy->List)
 * 3-entityObject:(entityname)->map(conditionKey+filedname->object)
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

	protected String getSessionKey() {
		return getName();
	}

	public void clear() {
		redisClearMap(getSessionKey());
	}

	public void clear(Object conditionKey) {
		redisRemoveMapFields(getSessionKey(), getRedisFieldKey(conditionKey, null));
	}

	public V remove(Object key) {
		return (V) redisDel(getSessionKey(), getRedisFieldKey(key));
	}

	public V remove(Object conditionKey, Object key) {
		return (V) redisDel(getSessionKey(), getRedisFieldKey(conditionKey, key));
	}

	public V get(GenericPK pk) {
		V value = (V) redisGet(getSessionKey(), pk.getPkShortValueString());
		return value;
	}

	public V get(Object conditionKey, Object key) {
		V value = (V) redisGet(getSessionKey(), getRedisFieldKey(conditionKey, key));
		return value;
	}

	public V put(K key, V value) {
		return (V) redisSet(getSessionKey(), getRedisFieldKey(key), value, (int) expireTimeNanos);
	}

	public V put(Object conditionKey, K key, V value) {
		return (V) redisSet(getSessionKey(), getRedisFieldKey(conditionKey, key), value, (int) expireTimeNanos);
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

	protected Object redisGet(String key, String field) {
		Jedis jedis = null;
		Boolean error = true;
		try {
			jedis = acquireRedisConnection();
			error = false;
			Object value = deserialize(jedis.hget(key.getBytes(), field.getBytes()));
			if (Debug.verboseOn())
				Debug.logVerbose("redis get with  key [" + key + "], field [" + field + "],result is [" + value + "]",
						"redis");
			return value;
		} finally {
			if (jedis != null) {
				returnRedisConnection(jedis, error);
			}
		}
	}

	protected Object redisSet(String key, Object value, int seconds) {
		Jedis jedis = null;
		Boolean error = true;
		try {
			jedis = acquireRedisConnection();
			error = false;
			if (seconds > 0) {
				jedis.setex(key.getBytes(), seconds, serialize(value));
			} else {
				jedis.set(key.getBytes(), serialize(value));
			}
			if (Debug.verboseOn())
				Debug.logVerbose("redis set with key [" + key + "], value is [" + value + "]"
						+ (seconds > 0 ? ", expire [" + seconds + "] seconds" : ""), "redis");
			return value;
		} finally {
			if (jedis != null) {
				returnRedisConnection(jedis, error);
			}
		}
	}

	protected Object redisSet(String key, String field, Object value, int seconds) {
		Jedis jedis = null;
		Boolean error = true;
		try {
			jedis = acquireRedisConnection();
			error = false;
			jedis.hset(key.getBytes(), field.getBytes(), serialize(value));
			if (seconds > 0) {
				// TOODO
			}
			if (Debug.verboseOn())
				Debug.logVerbose("redis set with key [" + key + "], field [" + seconds + "], value is [" + value + "]"
						+ (seconds > 0 ? ", expire [" + seconds + "] seconds" : ""), "redis");
			return value;
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

	protected Object redisDel(String key, String field) {
		Jedis jedis = null;
		Boolean error = true;
		try {
			jedis = acquireRedisConnection();
			error = false;
			Object oldValue = deserialize(jedis.hget(key.getBytes(), field.getBytes()));
			jedis.hdel(key.getBytes(), field.getBytes());
			if (Debug.verboseOn())
				Debug.logVerbose("redis del with key [" + key + "], field [" + field + "]", "redis");
			return oldValue;
		} finally {
			if (jedis != null) {
				returnRedisConnection(jedis, error);
			}
		}
	}
	

	protected void redisClearAll() {
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

	protected void redisClearMap(String sessionKey) {
		Jedis jedis = null;
		Boolean error = true;
		try {
			jedis = acquireRedisConnection();
			Set<byte[]> keySet = jedis.hkeys(sessionKey.getBytes());
			error = false;
			if (Debug.verboseOn())
				Debug.logVerbose("redis clear hashtable  with key [" + sessionKey + "]", "redis");
			for (byte[] kk : keySet) {
				jedis.del(kk);
			}
		} finally {
			if (jedis != null) {
				returnRedisConnection(jedis, error);
			}
		}
	}
	
	protected void redisRemoveMapFields(String sessionKey, String startwith) {
		Jedis jedis = null;
		Boolean error = true;
		try {
			jedis = acquireRedisConnection();
			Set<byte[]> keySet = jedis.hkeys(sessionKey.getBytes());
			error = false;
			if (Debug.verboseOn())
				Debug.logVerbose("redis clear hashtable  with key [" + sessionKey + "], start with ["+startwith+"]", "redis");
			for (byte[] kk : keySet) {
				if(new String(kk).startsWith(startwith)){
					jedis.del(kk);
				}			
			}
		} finally {
			if (jedis != null) {
				returnRedisConnection(jedis, error);
			}
		}
	}

	////////////////////////////////////////////////////////////////////

	protected String getRedisFieldKey(Object key) {
		StringBuffer sb = new StringBuffer();
		if (key instanceof GenericPK) { // PK
			sb.append(((GenericPK) key).getPkShortValueString());
		} else if (key instanceof EntityCondition) { // conditionKey
			sb.append(((EntityCondition) key).toString()); // where
		} else {
			sb.append(key);
		}
		return sb.toString();
	}

	protected String getRedisFieldKey(Object conditionKey, Object key) {
		StringBuffer sb = new StringBuffer();
		sb.append(conditionKey);
		if(key!=null){
			sb.append("_").append(key);
		}
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
