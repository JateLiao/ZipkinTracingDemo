package com.alibaba.apm;

import brave.Span;
import brave.handler.MutableSpan;
import brave.propagation.TraceContext;
import zipkin2.reporter.Reporter;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

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
    
    private static final String SPANHANDLER_FIELD_NAME = "finishedSpanHandler";
    private static final String STATE_FIELD_NAME = "state";
    private static final String HANDLE_METHOD_NAME = "handle";
    
    /**
     * 在异步，或者一个provider内多次调用外部服务，保证同一个线程内持有相同的parentId
     */
    protected static volatile InheritableThreadLocal<String> parentIdThreadLocal = new InheritableThreadLocal<>();
    protected static volatile InheritableThreadLocal<String> traceIdThreadLocal = new InheritableThreadLocal<>();
    
    protected static volatile Class realSpanClazz;
    //private static volatile Field finishedSpanHandlerField;
    //private static volatile Field stateField;
    
    static {
        try {
            realSpanClazz = Class.forName("brave.RealSpan");
            //finishedSpanHandlerField = realSpanClazz.getDeclaredField(SPANHANDLER_FIELD_NAME);
            //stateField = realSpanClazz.getDeclaredField(STATE_FIELD_NAME);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * span.finish()的反射调用实现
     * @param span span
     */
    protected void doSpanFinish(Span span) {
        if (null == span) {
            return;
        }
        Field handlerFld = null;
        Field stateFld = null;
        Method handleMethod = null;
        
        try {
            handlerFld = realSpanClazz.getDeclaredField(SPANHANDLER_FIELD_NAME);
            stateFld = realSpanClazz.getDeclaredField(STATE_FIELD_NAME);
            resetFieldAccessible(true, handlerFld, stateFld); // 重置字段访问权限
            
            TraceContext context = span.context();
            MutableSpan state = (MutableSpan) stateFld.get(span);
            resetFieldValue(state, "finishTimestamp", System.currentTimeMillis());
            
            if (null == handleMethod) {
                synchronized (Method.class) {
                    if (null == handleMethod) {
                        Method[] methods = handlerFld.get(span).getClass().getDeclaredMethods();
                        for (Method method : methods) {
                            if (method.getName().equals(HANDLE_METHOD_NAME)) {
                                handleMethod = method;
                                break;
                            }
                        }
                    }
                }
            }
            resetMethodAccessible(true, handleMethod);
            handleMethod.invoke(handlerFld.get(span), context, state);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 重置字段访问权限
            resetFieldAccessible(false, handlerFld, stateFld);
            resetMethodAccessible(false, handleMethod);
        }
    }
    
    /**
     * 重置字段访问权限
     * @param access 权限
     * @param fields 字段
     */
    private void resetFieldAccessible(boolean access, Field... fields) {
        if (null == fields || fields.length == 0) {
            return;
        }
        for (Field f : fields) {
            if (null == f) {
                continue;
            }
            f.setAccessible(access);
        }
    }
    
    /**
     * 重置字段访问权限
     * @param access 权限
     * @param methods 字段
     */
    private void resetMethodAccessible(boolean access, Method... methods) {
        if (null == methods || methods.length == 0) {
            return;
        }
        for (Method f : methods) {
            if (null == f) {
                continue;
            }
            f.setAccessible(access);
        }
    }
    
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
