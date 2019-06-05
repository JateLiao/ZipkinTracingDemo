package com.alibaba.apm;

import zipkin2.Span;
import zipkin2.reporter.Reporter;

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
    protected static final String TAG_KEY_PARAM = "param";
    protected static final String TAG_KEY_RESULT = "result";
    protected static final String TAG_KEY_METHOD = "method";
    protected static final String TAG_KEY_LOCALSERVICE = "localservice";
    protected static final String TAG_KEY_TYPE = "type";
    protected static final String TAG_KEY_SPANID = "spanId";
    protected static final String TAG_KEY_PARENTID = "parentId";
    protected static final String TAG_KEY_WHOLE_SPANNAME = "whole_span";
    
    /**
     * 在异步，或者一个provider内多次调用外部服务，保证同一个线程内持有相同的parentId
     */
    protected static volatile ThreadLocal<String> parentIdThreadLocal = new ThreadLocal<>();
    protected static volatile ThreadLocal<String> traceIdThreadLocal = new ThreadLocal<>();
    
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
