package com.alibaba.apm.filter;

import brave.Span;
//import com.alibaba.dubbo.common.utils.StringUtils;
//import com.alibaba.dubbo.rpc.RpcResult;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.*;

//import com.alibaba.dubbo.common.extension.Activate;
//import com.alibaba.dubbo.common.utils.StringUtils;
//import com.alibaba.dubbo.rpc.*;

/**
 * @author liaoshijie
 * @description: ${TODO}
 * @Createtime 2019/5/2416:17
 */
//@Activate
public class ProviderZipkinFilter extends AbstractZipkinFilter {
    
    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        System.out.println("【dubbo-filter】方法名: " + invocation.getMethodName());
        
        if (isEcho(invocation)) { // 过滤dubbo回声检测
            return new AsyncRpcResult(invocation);
        }
    
        // 构造span
        brave.Span span = buildSpanFromTracing(invocation);
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
    
    @Override
    protected void setSpanKind(Span span) {
        span.kind(Span.Kind.PRODUCER);
        span.tag(TAG_KEY_TYPE, Span.Kind.PRODUCER.name());
    }
}
