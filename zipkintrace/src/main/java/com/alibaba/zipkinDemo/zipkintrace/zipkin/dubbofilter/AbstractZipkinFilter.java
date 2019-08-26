package com.alibaba.zipkinDemo.zipkintrace.zipkin.dubbofilter;

import brave.Tracing;
import com.alibaba.zipkinDemo.zipkintrace.statics.BeanStatics;
import com.alibaba.zipkinDemo.zipkintrace.util.JsonUtils;
import com.alibaba.zipkinDemo.zipkintrace.zipkin.CommonZipkinHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.rpc.*;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import zipkin2.Span;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.Reporter;
import zipkin2.reporter.okhttp3.OkHttpSender;

/**
 * @author liaoshijie
 * @description: ${TODO}
 * @Createtime 2019/5/24 16:14
 */
@Slf4j
public abstract class AbstractZipkinFilter extends CommonZipkinHandler implements Filter, ApplicationContextAware {
    
    protected ApplicationContext applicationContext;
    
    /**
     // * 配置属性
     // */
    //@Setter
    //private ZipkinConfig zipkinConfig;
    
    /** 一堆常量 */
    protected static final String METHOD_ECHO = Constants.$ECHO; // dubbo回声检测的方法名
    protected static final String TRACE_ID_LONG_KEY = "zipkin-traceId-long";
    protected static final String PARENT_ID_LONG_KEY = "zipkin-parent-long";
    protected static final String TAG_KEY_LOCALSERVICE = "localservice";
    protected static final String TAG_KEY_WHOLE_SPANNAME = "whole_span";
    
    /**
     * tracing构造相关
     */
    private static volatile Reporter<Span> reporter;
    
    @Override
    public abstract Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException;
    
    /**
     * abstract 设置Span Kind信息
     * @param span span
     */
    protected abstract void setSpanKind(brave.Span span);
    
    /**
     * 判断是否为echo方法，逻辑来自dubbo的EchoFilter
     * @param invocation invocation
     * @return true/false
     */
    protected boolean isEcho(Invocation invocation) {
        String methodName = invocation.getMethodName();
        if (StringUtils.isNotEmpty(methodName)
                && METHOD_ECHO.equals(methodName)
                && invocation.getArguments() != null
                && invocation.getArguments().length == 1) { // echo方法是用来做心跳监测的？总之需要过滤
            return true;
        }
        
        return false;
    }
    
    /**
     * 设置tags，实现简单的日志功能，入参返回值以json形式上报
     * @param invocation invocation
     * @param result result
     * @param span span
     */
    protected void setTags(Invocation invocation, Result result, brave.Span span) {
        if (null == span) {
            log.error("【dubbo-filter】span异常，值为null");
            return;
        }
        setSpanKind(span); // 设置span类型信息，用以区分consumer/provider
        setSpanTag(span, TAG_KEY_METHOD, invocation.getMethodName());
        setSpanTag(span, TAG_KEY_PARAM, JsonUtils.toJsonWithJacksonYMDHms(invocation.getArguments()));
        setSpanTag(span, TAG_KEY_LOCALSERVICE, BeanStatics.zipkinConfig.getLocalService());
        setSpanTag(span, TAG_KEY_SPANID, String.valueOf(span.context().spanId()));
        setSpanTag(span, TAG_KEY_PARENTID, String.valueOf(span.context().parentId()));
        setSpanTag(span, TAG_KEY_THREADID, String.valueOf(Thread.currentThread().getId()));
        
        String spanName = invocation.getInvoker().getInterface().getName() + "/" + invocation.getMethodName(); // 完整span名，包括调用接口类路径+方法名
        setSpanTag(span, TAG_KEY_WHOLE_SPANNAME, spanName);
        
        // 返回值设置
        if (null != result) {
            Object valObj = result.getValue();
            if (null == valObj || valObj instanceof String) { // 字符串就不转json了
                setSpanTag(span, TAG_KEY_RESULT, String.valueOf(valObj));
            } else {
                setSpanTag(span, TAG_KEY_RESULT, JsonUtils.toJsonWithJacksonYMDHms(valObj));
            }
            
            // 服务端异常信息set
            span.error(result.getException());
        } else {
            setSpanTag(span, TAG_KEY_RESULT, "null");
        }
    }
    
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
        
        return Tracing.newBuilder().localServiceName(BeanStatics.zipkinConfig.getLocalService()).spanReporter(reporter).build();
    }
    
    /**
     * 从当前tracing获取一个span
     * @param invocation invocation
     * @return span
     */
    protected brave.Span buildSpanFromTracing(Invocation invocation) {
        Tracing tracing = getTracing();
        tracing.tracer().startScopedSpan(getScopeSpanName(invocation));
        return tracing.tracer().currentSpan().start(System.currentTimeMillis());
    }
    
    /**
     * 获取scopedSpan名：接口名 + 方法名
     * @param invocation invocation
     * @return name
     */
    protected String getScopeSpanName(Invocation invocation) {
        String[] arr = invocation.getInvoker().getInterface().getName().split("\\.");
        return arr[arr.length - 1] + "/" + invocation.getMethodName();
    }
    
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
    
    /**
     * 记录dubbo日志
     * @param invocation invoke
     * @param result res
     * @param type type
     * @param traceId traceId
     */
    protected void writeDubboLog(Invocation invocation, Result result, String type, String traceId) {
        try {
            //LogHelper logHelper = applicationContext.getBean(LogHelper.class);
            BeanStatics.logHelper.writeWebServiceLog(invocation, result, type, traceId);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
