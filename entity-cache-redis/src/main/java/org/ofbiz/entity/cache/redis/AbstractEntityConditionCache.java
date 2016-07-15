/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package org.ofbiz.entity.cache.redis;

import java.util.Iterator;
import java.util.Map;

import org.ofbiz.base.cache.redis.UtilRedisCache;
import org.ofbiz.base.cache.redis.UtilRedisCacheFactory;
import org.ofbiz.base.util.Debug;
//import org.ofbiz.base.util.cache.UtilCache;
import org.ofbiz.entity.GenericEntity;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.model.ModelEntity;

/**
 * copy from org.ofbiz.entity.cache.AbstractEntityConditionCache remove method
 * storeHook
 */
public abstract class AbstractEntityConditionCache<K, V> extends AbstractCache<K, V> {

	public static final String module = AbstractEntityConditionCache.class.getName();

	protected AbstractEntityConditionCache(String delegatorName, String id) {
		super(delegatorName, id);
	}

	protected V get(String entityName, EntityCondition condition, K key) {
		UtilRedisCache<K, V> cache = getCache(entityName);
		if (cache == null)
			return null;
		return cache.get(getConditionKey(condition), key);
	}

	protected V put(String entityName, EntityCondition condition, K key, V value) {
		ModelEntity entity = this.getDelegator().getModelEntity(entityName);
		if (entity.getNeverCache()) {
			Debug.logWarning("Tried to put a value of the " + entityName
					+ " entity in the cache but this entity has never-cache set to true, not caching.", module);
			return null;
		}

		UtilRedisCache<K, V> cache = getCache(entityName);
		if (cache == null)
			return null;

		return cache.put(condition, key, value);
	}

	/**
	 * Removes all condition caches that include the specified entity.
	 */
	public void remove(GenericEntity entity) {
		UtilRedisCacheFactory.clearCache(getCacheName(entity.getEntityName()));
		ModelEntity model = entity.getModelEntity();
		if (model != null) {
			Iterator<String> it = model.getViewConvertorsIterator();
			while (it.hasNext()) {
				String targetEntityName = it.next();
				UtilRedisCacheFactory.clearCache(getCacheName(targetEntityName));
			}
		}
	}

	public void remove(String entityName, EntityCondition condition) {
		UtilRedisCache<K, V> cache = getCache(entityName);
		if (cache == null)
			return;
		cache.clear(condition);
	}

	protected V remove(String entityName, EntityCondition condition, K key) {
		UtilRedisCache<K, V> cache = getCache(entityName);
		if (cache == null)
			return null;
		return cache.remove(condition, key);
	}

	public static final EntityCondition getConditionKey(EntityCondition condition) {
		return condition != null ? condition : null;
	}

	public static final EntityCondition getFrozenConditionKey(EntityCondition condition) {
		EntityCondition frozenCondition = condition != null ? condition.freeze() : null;
		return frozenCondition;
	}

	protected static final <K, V> boolean isNull(Map<K, V> value) {
		return value == null || value == GenericEntity.NULL_ENTITY || value == GenericValue.NULL_VALUE;
	}

}
