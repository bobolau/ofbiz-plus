

 (cluster) tomcat session management by redis

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