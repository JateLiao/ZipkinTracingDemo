package com.alibaba.zipkinDemo.zipkintrace.zipkin.springbootinterceptor;

import brave.Span;
import brave.Tracing;
import com.alibaba.zipkinDemo.zipkintrace.config.ZipkinConfig;
import com.alibaba.zipkinDemo.zipkintrace.statics.BeanStatics;
import com.alibaba.zipkinDemo.zipkintrace.zipkin.CommonZipkinHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.Reporter;
import zipkin2.reporter.okhttp3.OkHttpSender;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author liaoshijie
 * @description: ${TODO}
 * @Createtime 2019/6/3 18:22
 */
@Slf4j
@Component
public abstract class AbstractZipkinInterceptor extends CommonZipkinHandler implements HandlerInterceptor {
    
    /**
     // * 配置属性
     // */
    /** 一堆常量 */
    protected static final String TAG_KEY_WHOLE_URL = "whole_url";
    protected static final String TAG_KEY_PRE_ERROR = "pre-error";
    protected static final String TAG_KEY_AFTER_ERROR = "after-error";
    
    /**
     // * 配置属性
     // */
    @Autowired
    private ZipkinConfig zipkinConfig;
    
    /**
     * tracing构造相关
     */
    protected static volatile Reporter<zipkin2.Span> reporter;
    
    @Override
    protected abstract void setSpanKind(Span span);
    
    @Override
    public abstract boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception;
    
    @Override
    public abstract void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception;
    
    @Override
    public abstract void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception;
    
    /**
     * 获取tracing
     * @return Tracing实例
     */
    protected Tracing getTracing() {
        if (reporter == null) {
            synchronized (Reporter.class) {
                if (reporter == null) {
                    OkHttpSender sender = OkHttpSender.newBuilder().endpoint(BeanStatics.zipkinConfig.getEndpoint()).build(); // 构建数据发送对象
                    reporter = AsyncReporter.builder(sender).build(); // 构建数据上报对象
                }
            }
        }
        
        return Tracing.newBuilder().localServiceName(BeanStatics.zipkinConfig.getLocalService() + "-api").spanReporter(reporter).build();
    }
    
    protected brave.Span buildSpanFromTracing(String spanName, Tracing tracing, boolean isNewTrace) {
        tracing.tracer().startScopedSpan(spanName);
        Span span = isNewTrace ? tracing.tracer().newTrace() : tracing.tracer().currentSpan();
        return span.start(System.currentTimeMillis());
    }
}
