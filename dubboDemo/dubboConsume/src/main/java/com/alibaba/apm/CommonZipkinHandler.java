package com.alibaba.apm;

import brave.Tracing;
import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Result;
import zipkin2.Span;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.Reporter;
import zipkin2.reporter.okhttp3.OkHttpSender;

import java.lang.reflect.Field;

/**
 * @Title: ${FILE_NAME}
 * @Company: com.lsj
 * @Package com.alibaba.apm
 * @Description: ${TODO}
 * @Author liao
 * @Createtine 2019/6/222:14
 */
public abstract class CommonZipkinHandler {
    
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
    
    protected static final String localservice = "consumer";
    protected static final String endpoint = "http://tracing-analysis-dc-hz.aliyuncs.com/adapt_bfciltjavz@d33dad698d04891_bfciltjavz@53df7ad2afe8301/api/v2/spans";
    
    /**
     * 在异步，或者一个provider内多次调用外部服务，保证同一个线程内持有相同的parentId
     */
    protected static ThreadLocal<String> parentIdThreadLocal = new ThreadLocal<>();
    protected static ThreadLocal<String> traceIdThreadLocal = new ThreadLocal<>();
    
    /**
     * tracing构造相关
     */
    //private static volatile Tracing tracing;
    private static volatile Reporter<Span> reporter;
    
    /**
     * abstract 设置Span Kind信息
     * @param span span
     */
    protected abstract void setSpanKind(brave.Span span);
    
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
    
    protected brave.Span buildSpanFromTracing(String spanName, Tracing tracing) {
        tracing.tracer().startScopedSpan(spanName);
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
