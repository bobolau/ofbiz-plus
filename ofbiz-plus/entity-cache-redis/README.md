
 (cluster) entity cache by redis
preload ofbiz-plus-*.jar(for replace class org.ofbiz.entity.cache.Cache)
	/framework/base/config/cache.properties

		entitycache.redis-host=localhost
		entitycache.redis-port=6379
		entitycache.redis-database=0
		entitycache.redis-password=
		entitycache.redis-timeout=2000
		entitycache.redis-sentinelMaster=