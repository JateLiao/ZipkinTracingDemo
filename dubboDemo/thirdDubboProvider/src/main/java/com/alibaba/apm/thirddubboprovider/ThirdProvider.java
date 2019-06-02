package com.alibaba.apm.thirddubboprovider;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Administrator
 * @description: ${TODO}
 * @Createtime 2019/5/2217:24
 */
public class ThirdProvider {
    /**
     * main method.
     **/
    public static void main(String[] args) throws Exception {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("META-INF/spring/dubbo-demo-third-provider.xml");
        context.start();
    
        System.in.read(); // 按任意键退出
        System.out.println();
    }
}
