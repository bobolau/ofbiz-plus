ofbiz集成redis实现tomcat会话同步
----------------------------------- 

特性描述：
----------------------------------- 
用redis实现tomcat会话同步，以提供tomcat集群多主机时会话同步的效率。
主要实现ofbiz集成此功能，原功能实现参考

 redis实现tomcat会话同步
 https://github.com/jcoleman/tomcat-redis-session-manager

处理步骤：
----------------------------------- 
### 1. 复制基础jar包
复制/ofbiz-plus/lib/下jar到／ofbiz/apache-ofbiz-xxx/framework/base/lib/
  commons-pool2-2.3.jar
  jedis-2.8.0.jar

### 2. 复制实现jar包
复制/ofbiz-plus/tomcat-session-redis/build/libs/ofbiz-tomcat-session-redis-xx.jar
到/ofbiz/apache-ofbiz-xxx/framework/catalina/build/lib/

### 3. 修改配置文件
/ofbiz/apache-ofbiz-xxx/framework/catalina/ofbiz-component.xml (13.07及以后版本)  <br />
12.04之前版本 /framework/base/config/ofbiz-containers.xml  <br />
修改配置内容，在cluster配置中修改redis参数
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
