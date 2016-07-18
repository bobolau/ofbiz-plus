Ofbiz Plus
===================================

基于Ofbiz扩展的快速部署大并发电商架构，提供源代码和Docker运行模式



ofbiz plus 1.2 (dev)
----------------------------------- 
### ofbiz plus容器部署
1nginx+2ofbiz+1oauth+1redis+1mysql
(docker+k8s)
### bug fix ofbiz
下单并发锁表问题等 

ofbiz plus 1.1 (dev)
----------------------------------- 
### ofbiz plus dockerfile
提供ofbiz的dockerfile
### rest for ofbiz service
通过spring mvc包装ofbiz服务为rest服务
### auth by OAuth2
通过spring security oauth提供认证服务

ofbiz plus 1.0 (release)
----------------------------------- 
引入redis提供集群扩展功能
### redis实现tomcat会话复制
ofbiz集群－通过redis实现tomcat的会话复制功能 <br />
参见 [tomcat-session-redis](https://github.com/bobolau/agile-biz/tree/ofbiz-plus1.0/ofbiz-plus/tomcat-session-redis) <br />
### redis实现entitycache
ofbiz集群－通过redis实现entitycache功能（替换通过MQ实现缓存同步功能）<br />
参见 [entity-cache-redis](https://github.com/bobolau/agile-biz/tree/ofbiz-plus1.0/ofbiz-plus/entity-cache-redis) <br />
