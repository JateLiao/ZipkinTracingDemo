package com.alibaba.apm.filter;

import brave.Span;
import brave.handler.MutableSpan;
import brave.propagation.TraceContext;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.*;
//import com.alibaba.dubbo.common.extension.Activate;
//import com.alibaba.dubbo.common.utils.StringUtils;
//import com.alibaba.dubbo.rpc.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * @author liaoshijie
 * @description: ${TODO}
 * @Createtime 2019/5/2416:17
 */
@Activate
public class ProviderZipkinFilter extends AbstractZipkinFilter {
    
    private static final String SPANHANDLER_FIELD_NAME = "finishedSpanHandler";
    private static final String STATE_FIELD_NAME = "state";
    private static final String HANDLE_METHOD_NAME = "handle";
    
    private static volatile Class realSpanClazz;
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
    
    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        System.out.println("【dubbo-filter】方法名: " + invocation.getMethodName());
        
        if (isEcho(invocation)) { // 过滤dubbo回声检测
            return new AsyncRpcResult(invocation);
        }
    
        // 构造span
        Span span = buildSpanFromTracing(invocation);
        if (null == span) {
            throw new RpcException("【dubbo-filter】Span构造失败");
        }
        
        Result result = null;
        try {
            String currentTraceIdL = String.valueOf(span.context().traceId());
            String traceIdInvoke = RpcContext.getContext().getAttachments().get(TRACE_ID_LONG_KEY);
            
            // 当前traceid和接收到的traceid不一样，则需要重置，为了形成链路
            if (StringUtils.isEmpty(traceIdInvoke) || !traceIdInvoke.equals(currentTraceIdL)) {
                resetFieldValue(span.context(), SPAN_CONTEXT_TRACEID_FIELD_NAME, Long.parseLong(traceIdInvoke));
                RpcContext.getContext().getAttachments().put(TRACE_ID_LONG_KEY, traceIdInvoke); // 向后传递traceid
            }
    
            // parentId设置，保证层级关系
            String parentIdInvoke = RpcContext.getContext().getAttachments().get(PARENT_ID_LONG_KEY);
            if (StringUtils.isNotEmpty(parentIdInvoke)) {
                resetFieldValue(span.context(), SPAN_CONTEXT_PARENT_FIELD_NAME, Long.parseLong(parentIdInvoke));
            }
            RpcContext.getContext().getAttachments().put(PARENT_ID_LONG_KEY, String.valueOf(span.context().spanId()));
            parentIdThreadLocal.set(String.valueOf(span.context().spanId()));
            
            result = invoker.invoke(invocation);
        } catch (Exception e) {
            e.printStackTrace();
            span.error(e);
        } finally {
            setTags(invocation, result, span);
            //span.finish();
            doSpanFinish(span);
            parentIdThreadLocal.remove(); // 阅后即焚
        }
        
        return result;
    }
    
    /**
     * span.finish()的反射调用实现
     * @param span span
     */
    private void doSpanFinish(Span span) {
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
    
    @Override
    protected void setSpanKind(Span span) {
        span.kind(Span.Kind.PRODUCER);
        span.tag(TAG_KEY_TYPE, Span.Kind.PRODUCER.name());
    }
}
