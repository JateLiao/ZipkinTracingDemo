package com.alibaba.apm.interceptor;

import brave.Span;
import brave.Tracing;
import com.alibaba.apm.CommonZipkinHandler;
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
 * @Createtime 2019/6/3 17:55
 */
public abstract class AbstractZipkinInterceptor extends CommonZipkinHandler implements HandlerInterceptor {
    protected static final String localservice = "consumer";
    protected static final String endpoint = "http://tracing-analysis-dc-hz.aliyuncs.com/adapt_bfciltjavz@d33dad698d04891_bfciltjavz@53df7ad2afe8301/api/v2/spans";
    
    
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
                    OkHttpSender sender = OkHttpSender.newBuilder().endpoint(endpoint).build(); // 构建数据发送对象
                    reporter = AsyncReporter.builder(sender).build(); // 构建数据上报对象
                }
            }
        }
        
        return Tracing.newBuilder().localServiceName(localservice + "-api").spanReporter(reporter).build();
    }
    
    protected brave.Span buildSpanFromTracing(String spanName, Tracing tracing) {
        tracing.tracer().startScopedSpan(spanName);
        //return tracing.tracer().currentSpan();
        return tracing.tracer().newTrace();
    }
}
