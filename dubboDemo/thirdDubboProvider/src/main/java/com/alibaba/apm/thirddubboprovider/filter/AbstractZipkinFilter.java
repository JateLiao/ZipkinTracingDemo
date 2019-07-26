package com.alibaba.apm.thirddubboprovider.filter;

import brave.Tracing;
//import com.alibaba.dubbo.common.Constants;
//import com.alibaba.dubbo.common.extension.Activate;
//import com.alibaba.dubbo.common.utils.StringUtils;
//import com.alibaba.dubbo.rpc.*;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.*;
import zipkin2.Span;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.Reporter;
import zipkin2.reporter.okhttp3.OkHttpSender;

import java.lang.reflect.Field;

/**
 * @author liaoshijie
 * @description: ${TODO}
 * @Createtime 2019/5/2416:14
 */
@Activate
public abstract class AbstractZipkinFilter implements Filter {
    
    /**
    // * 配置属性
    // */
    
    /** 一堆常量 */
    protected static final String SPAN_CONTEXT_TRACEID_FIELD_NAME = "traceId";
    protected static final String SPAN_CONTEXT_PARENT_FIELD_NAME = "parentId";
    protected static final String METHOD_ECHO = Constants.$ECHO; // dubbo回声检测的方法名
    protected static final String TRACE_ID_LONG_KEY = "zipkin-traceId-long";
    protected static final String PARENT_ID_LONG_KEY = "zipkin-parent-long";
    protected static final String TAG_KEY_PARAM = "param";
    protected static final String TAG_KEY_RESULT = "result";
    protected static final String TAG_KEY_METHOD = "method";
    protected static final String TAG_KEY_LOCALSERVICE = "localservice";
    protected static final String TAG_KEY_TYPE = "type";
    protected static final String TAG_KEY_SPANID = "spanId";
    protected static final String TAG_KEY_PARENTID = "parentId";
    protected static final String TAG_KEY_WHOLE_SPANNAME = "whole_span";
    
    protected static final String localservice = "commonLogService-provider";
    protected static final String endpoint = "http://tracing-analysis-dc-hz.aliyuncs.com/adapt_bfciltjavz@d33dad698d04891_bfciltjavz@53df7ad2afe8301/api/v2/spans";
    
    /**
     * 在异步，或者一个provider内多次调用外部服务，保证同一个线程内持有相同的parentId
     */
    protected static ThreadLocal<String> parentIdThreadLocal = new ThreadLocal<>();
    
    /**
     * tracing构造相关
     */
    //private static volatile Tracing tracing;
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
            return;
        }
        setSpanKind(span); // 设置span类型信息，用以区分consumer/provider
        setSpanTag(span, TAG_KEY_METHOD, invocation.getMethodName());
        setSpanTag(span, TAG_KEY_PARAM, invocation.getArguments().toString());
        setSpanTag(span, TAG_KEY_LOCALSERVICE, localservice);
        setSpanTag(span, TAG_KEY_SPANID, String.valueOf(span.context().spanId()));
        setSpanTag(span, TAG_KEY_PARENTID, String.valueOf(span.context().parentId()));
    
        String spanName = invocation.getInvoker().getInterface().getName() + "/" + invocation.getMethodName(); // 完整span名，包括调用接口类路径+方法名
        setSpanTag(span, TAG_KEY_WHOLE_SPANNAME, spanName);
        
        // 返回值设置
        if (null != result) {
            Object valObj = result.getValue();
            if (null == valObj || valObj instanceof String) { // 字符串就不转json了
                setSpanTag(span, TAG_KEY_RESULT, String.valueOf(valObj));
            } else {
                setSpanTag(span, TAG_KEY_RESULT, String.valueOf(valObj));
            }
            
            // 服务端异常信息set
            span.error(result.getException());
        } else {
            setSpanTag(span, TAG_KEY_RESULT, "null");
        }
    }
    
    /**
     * setSpanTag.
     * @param span span
     * @param key key
     * @param value value
     */
    protected void setSpanTag(brave.Span span, String key, String value) {
        span.tag(key, value);
    }
    
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
    
        return Tracing.newBuilder().localServiceName(localservice).spanReporter(reporter).build();
    }
    
    /**
     * 从当前tracing获取一个span
     * @param invocation invocation
     * @return span
     */
    protected brave.Span buildSpanFromTracing(Invocation invocation) {
        Tracing tracing = getTracing();
        tracing.tracer().startScopedSpan(getScopeSpanName(invocation));
        return tracing.tracer().currentSpan();
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
    
    /**
     * 重设对象字段值
     * @param object object
     * @param fieldName name
     * @param value value
     * @throws Exception e
     */
    protected void resetFieldValue(Object object, String fieldName, Object value) throws Exception {
        Field parentIdFld = object.getClass().getDeclaredField(fieldName);
        parentIdFld.setAccessible(true); // traceId字段为final类型
        parentIdFld.set(object, value);
        parentIdFld.setAccessible(false);
    }
}
