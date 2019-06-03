package com.alibaba.apm.config;

import com.alibaba.apm.interceptor.ZipkinInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author liaoshijie
 * @description: ${TODO}
 * @Createtime 2019/6/3 15:43
 */
@Configuration
public class WebAppConfigurer implements WebMvcConfigurer {
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new ZipkinInterceptor()).addPathPatterns("/**");
    }
}
