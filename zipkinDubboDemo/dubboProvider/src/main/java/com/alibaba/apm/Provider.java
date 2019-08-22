package com.alibaba.apm;

import org.springframework.context.support.ClassPathXmlApplicationContext;

//@SpringBootApplication
//@ComponentScan(value = {"com.alibaba.apm"})
public class Provider {


    /**
     * To get ipv6 address to work, add
     * System.setProperty("java.net.preferIPv6Addresses", "true");
     * before running your application.
     */
    public static void main(String[] args) throws Exception {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("META-INF/spring/dubbo-demo-provider.xml");
        context.start();
    
        System.in.read(); // 按任意键退出
        System.out.println();
    }


}
