package com.alibaba.apm.controller;

import com.alibaba.apm.DemoService;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author liaoshijie
 * @description: ${TODO}
 * @Createtime 2019/6/3 14:49
 */
@RestController
@RequestMapping("/api/consumer")
public class ConsumerController {
    
    private static DemoService demoService;
    
    static {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"META-INF/spring/dubbo-demo-consumer.xml"});
        context.start();
        demoService = (DemoService) context.getBean("demoService");
    }
    
    @RequestMapping("/getinfo")
    @ResponseBody
    public String getInfo(HttpServletRequest request, @RequestParam(value = "param") String param) {
        String val = "";
        for (int i = 0; i < 3; i++) {
            val += "controller for param: " + param + ", value: " + demoService.sayHello(param) + ", " + System.currentTimeMillis();
        }
        
        //Timer timer = new Timer("TestConsumerTimer", true);
        //timer.scheduleAtFixedRate(new TimerTask() {
        //    @Override
        //    public void run() {
        //        demoService.sayHello("consumer的timer测试入参--" + Thread.currentThread().getId());
        //    }
        //}, 0, 10);
        
        ExecutorService service = Executors.newFixedThreadPool(5);
        for (int i = 0; i < 5; i++) {
            service.execute(() -> {
                demoService.sayHello("consumer的ExecutorService测试入参--" + Thread.currentThread().getId());
            });
        }
        service.shutdown();
        while (!service.isTerminated()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        
        return val;
    }
}