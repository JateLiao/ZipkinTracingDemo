## WebMVC 4 Boot Example

Instead of servlet, this uses Spring Boot 1.5 to create a self-contained
application that runs Spring WebMVC 4 controllers.

*    修改TracingConfiguration类中sender的值，配置对应的网关，用户名，用户key。
* 启动前台服务， 运行$ mvn compile exec:java -Dexec.mainClass=brave.webmvc.Backend
* 启动后台服务， 运行$ mvn compile exec:java -Dexec.mainClass=brave.webmvc.Frontend
*  访问http://localhost:8081