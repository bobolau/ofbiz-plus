

ofbiz plus mean orgin ofbiz plus enhance features as follow,
1. (cluster) tomcat session management by redis

12.04
/framework/base/config/ofbiz-containers.xml
13.07
/framework/catalina/ofbiz-component.xml

    <container name="catalina-container" loaders="main" class="org.ofbiz.catalina.container.CatalinaContainerRedis">
    
        <property name="default-server" value="engine">  
                     
            <property name="redis-cluster" value="cluster">
                <property name="redis-host" value="localhost"/>
                <property name="redis-port" value="6379"/>
                <property name="redis-database" value="0"/>
                <property name="redis-password" value=""/>
                <property name="redis-timeout" value="2000"/>
                <property name="redis-sentinelMaster" value=""/>
            </property>
            
        </property>
    </container>

2. (cluster) entity cache by redis
preload ofbiz-plus-*.jar(for replace class org.ofbiz.entity.cache.Cache)
/framework/base/config/cache.properties
entitycache.redis-host=localhost
entitycache.redis-port=6379
entitycache.redis-database=0
entitycache.redis-password=
entitycache.redis-timeout=2000
entitycache.redis-sentinelMaster=




3. rest API expose by spring mvc
4. rest API schema by swagger


