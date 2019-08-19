package com.alibaba.zipkinDemo.zipkintrace.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class AliLogConfig {
    
    //---------------- AccessKey ----------------//
    @Value("${aliyun.accessKeyId}")
    private String accessKeyId;
    
    @Value("${aliyun.accessSecret}")
    private String accessKeySecret;
    
    //---------------- LOG ----------------//
    @Value("${aliyun.log.endpoint}")
    private String logEndpoint;
    
    @Value("${aliyun.log.project}")
    private String logProject;
    
    @Value("${aliyun.log.serverLogstore}")
    private String serverLogstore;
}
