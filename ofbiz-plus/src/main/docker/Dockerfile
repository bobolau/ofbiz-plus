FROM java:8

# copy ofbiz
VOLUME /usr/ofbiz
COPY /ofbiz/apache-ofbiz-13.07.03  /usr/ofbiz/

# copy ofbiz-plus
VOLUME /usr/ofbiz
ADD /ofbiz-plus/build/libs/ofbiz-tomcat-session-redis-1.0.jar /usr/ofbiz/framework/base/lib/plus/ofbiz-tomcat-session-redis.jar
ADD /ofbiz-plus/build/libs/ofbiz-entity-cache-redis-1.0.jar /usr/ofbiz/framework/base/lib/plus/ofbiz-entity-cache-redis.jar

# startup/stop
COPY ofbiz.sh /etc/init.d/ofbiz
ENTRYPOINT /etc/init.d/ofbiz