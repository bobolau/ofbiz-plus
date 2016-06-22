package org.ofbiz.base.cache.redis;

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
		Set<String> redisKeys = getRedisKeys();
		// TODO clear all keys
	}

	public V remove(Object key) {
		String redisKey = getRedisKey(key);
		// TODO
		if (isConditionCache(key)) {
			// ConcurrentMap<Object, List<GenericValue>>
		} else {

		}
		return null;
	}

	public Set<? extends K> getCacheLineKeys() {
		// TODO only for condition key
		return null;
	}

	public V get(Object key) {
		String redisKey = getRedisKey(key);
		// TODO
		if (isConditionCache(key)) {
			// value: Map<Object, List<GenericValue>>, Map<Object, Object>
		} else {
			// value: GenericValue
		}
		return null;
	}

	public V put(K key, V value) {
		String redisKey = getRedisKey(key);
		// TODO
		if (isConditionCache(key)) {
			// value: Map<Object, List<GenericValue>>, Map<Object, Object>
		} else {
			// value: GenericValue

		}
		return null;
	}

	////////////////////////////////////////////////////////////////////

	protected boolean isConditionCache(Object key) {
		if (key != null && key instanceof EntityCondition) {
			return true;
		}
		return false;
	}

	protected String getRedisKey(Object key) {
		StringBuffer sb = new StringBuffer();
		sb.append(getKeyPrefix());
		if (key instanceof GenericPK) { // PK
			for (ModelField curPk : ((GenericPK) key).getModelEntity().getPkFieldsUnmodifiable()) {
				sb.append("_").append(((GenericPK) key).getString(curPk.getName()));
			}
		} else if (key instanceof EntityCondition) { // conditionKey
			sb.append(((EntityCondition) key).toString()); // where
		} else {
			sb.append(key);
		}
		return sb.toString();
	}

	protected Set<String> getRedisKeys() {
		// TODO
		return null;
	}

	protected void setPropertiesParams(String cacheName) {
		setPropertiesParams(new String[] { cacheName });
	}

	protected void setPropertiesParams(String[] propNames) {
		ResourceBundle res = ResourceBundle.getBundle("cache");
		if (res != null) {
			String value = getPropertyParam(res, propNames, "expireTime");
			if (UtilValidate.isNotEmpty(value)) {
				this.expireTimeNanos = TimeUnit.NANOSECONDS.convert(Long.parseLong(value), TimeUnit.MILLISECONDS);
			}
		}
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
					module);
		}
		return null;
	}

}
