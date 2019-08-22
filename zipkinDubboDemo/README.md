## dubbo-zipkin

### 运行步骤

*   修改endpoint(dubbo-demo-provider.xml 和 dubbo-demo-consumer.xm)值配置对应的网关，用户名，用户key。
*   启动服务端程序。运行com.alibaba.apm.Provider
*   启动客户端程序。运行com.alibaba.apm.Consumer

如果报错 " java.lang.IllegalStateException: Can't assign requested address
   	at com.alibaba.dubbo.registry.multicast.MulticastRegistry.<init>(MulticastRegistry.java:117)"
需要在启动的vm参数中添加 -Djava.net.preferIPv4Stack=true


