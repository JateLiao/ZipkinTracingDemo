package com.alibaba.apm;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import zipkin2.reporter.okhttp3.OkHttpSender;

import java.sql.Time;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
public class Consumer {
    
    
    /**
     * To get ipv6 address to work, add
     * System.setProperty("java.net.preferIPv6Addresses", "true");
     * before running your application.
     */
    public static void main(String[] args) {
        SpringApplication.run(Consumer.class,
                "--spring.application.name=consumer",
                "--server.port=9000",
                "--spring.aop.auto=true"
        );
        
        //System.setProperty("java.net.preferIPv4Stack", "true");
        //ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"META-INF/spring/dubbo-demo-consumer.xml"});
        //context.start();
        //
        //DemoService demoService = (DemoService) context.getBean("demoService"); // get remote service proxy
        //
        //for (int i = 0; i < 2; i++) {
        //    new Thread(() -> {
        //        String helloss = demoService.sayHello("world"); // call remote method
        //        System.out.println(helloss);
        //    }).start();
        //
        //
        //    //String helloss = demoService.sayHello("world"); // call remote method
        //    //System.out.println(helloss);
        //}
        //
        //try {
        //    Thread.sleep(TimeUnit.SECONDS.toMillis(5));
        //} catch (InterruptedException e) {
        //    e.printStackTrace();
        //}
        //
        //try {
        //    Thread.sleep(TimeUnit.SECONDS.toMillis(10));
        //} catch (InterruptedException e) {
        //    e.printStackTrace();
        //}
    
        //ExecutorService service = null;
        //while (true) {
        //    try {
        //        service = Executors.newFixedThreadPool(100);
        //        for (int i = 0; i < 150; i++) {
        //            service.execute(() -> {
        //                String hello = demoService.sayHello("world"); // call remote method
        //                System.out.println();
        //            });
        //        }
        //
        //        service.shutdown();
        //        while (!service.isTerminated()) {
        //            Thread.sleep(TimeUnit.SECONDS.toMillis(1));
        //        }
        //
        //        Thread.sleep(5000);
        //    } catch (Exception e) {
        //        e.printStackTrace();
        //        try {
        //            Thread.sleep(3000);
        //        } catch (InterruptedException eee) {
        //            eee.printStackTrace();
        //        }
        //    }
        //}
        ///////////////
    }
}
