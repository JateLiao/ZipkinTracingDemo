package com.alibaba.zipkinDemo.zipkintrace.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * @author liaoshijie
 * @description: ${TODO}
 * @Createtime 2019/5/30 14:18
 */
@Configuration
@Data
@Component
public class ZipkinConfig {
    
    static {
        System.out.println("你特么倒是初始化啊");
    }
    
    //@Value("${aliyun.zipkin.endpoint}")
    private String endpoint;
    
    //@Value("${aliyun.zipkin.localService}")
    private String localService;
    
}
