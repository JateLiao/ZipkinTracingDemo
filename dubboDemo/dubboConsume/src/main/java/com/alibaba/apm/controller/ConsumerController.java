package com.alibaba.apm.controller;

import com.alibaba.apm.DemoService;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

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
        return "controller for param: " + param + ", value: " + demoService.sayHello(param);
    }
}
