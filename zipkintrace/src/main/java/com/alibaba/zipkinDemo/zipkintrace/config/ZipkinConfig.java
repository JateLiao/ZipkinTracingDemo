package com.alibaba.zipkinDemo.zipkintrace.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * @author liaoshijie
 * @description: ${TODO}
 * @Createtime 2019/5/30 14:18
 */
@Configuration
@Data
public class ZipkinConfig {
    
    @Value("${aliyun.zipkin.endpoint}")
    private String endpoint;
    
    @Value("${aliyun.zipkin.localService}")
    private String localService;
    
}
