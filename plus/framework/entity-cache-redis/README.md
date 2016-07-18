redis实现实体缓存entitycache
----------------------------------------------

特性描述：
----------------------------------------------
 用redis实现entitycache缓存，在集群部署下可去掉原有缓存在集群内的同步机制。
 (cluster) entity cache by redis
处理步骤：
----------------------------------------------
### 1. 复制基础jar包
复制/ofbiz-plus/lib/下jar到／ofbiz/apache-ofbiz-xxx/framework/base/lib/
commons-pool2-2.3.jar
jedis-2.8.0.jar

### 2. 复制实现jar包
复制/ofbiz-plus/entity-cache-redis/build/libs/ofbiz-entity-cache-redis-xx.jar
到／ofbiz/apache-ofbiz-xxx/framework/base/build/lib/  <br />
特别注意因ofbiz的EntityCache非接口方式实现，需要替换jar在原entitycache的class前载入。 <br/>
preload ofbiz-plus-*.jar(for replace class org.ofbiz.entity.cache.Cache)

### 3. 新增配置文件
复制/ofbiz-plus/entity-cache-redis/src/main/resources/entitycache.properties
到／ofbiz/apache-ofbiz-xxx/framework/base/config <br/>

文件中entitycache.properties可配置redis的连接以及原有cache.properties中entitycache的设置 <br/>

		entitycache.redis-host=localhost
		entitycache.redis-port=6379
		entitycache.redis-database=0
		entitycache.redis-password=
		entitycache.redis-timeout=2000
		entitycache.redis-sentinelMaster=
