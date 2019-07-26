package com.alibaba.apm.thirddubboprovider.filter;

import brave.Span;
//import com.alibaba.dubbo.common.extension.Activate;
//import com.alibaba.dubbo.common.utils.StringUtils;
//import com.alibaba.dubbo.rpc.*;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.*;

/**
 * @author liaoshijie
 * @description: ${TODO}
 * @Createtime 2019/5/2416:17
 */
@Activate
public class ConsumerZipkinFilter extends AbstractZipkinFilter {
    
    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        System.out.println("【dubbo-filter】方法名: " + invocation.getMethodName());
        
        if (isEcho(invocation)) { // 过滤dubbo回声检测
            return new AsyncRpcResult(invocation);
        }
        
        // 构造span
        Span span = buildSpanFromTracing(invocation);
        if (null == span) {
            System.out.println("【dubbo-filter】Span构造失败");
            throw new RpcException("【dubbo-filter】Span构造失败");
        }
        
        Result result = null;
        try {
            String currentTraceIdL = String.valueOf(span.context().traceId());
            String traceIdInvoke = RpcContext.getContext().getAttachments().get(TRACE_ID_LONG_KEY);
            if (StringUtils.isEmpty(traceIdInvoke) || !traceIdInvoke.equals(currentTraceIdL)) { // 为空一般表示链路起点
                //RpcContext.getContext().getAttachments().put(TRACE_ID_LONG_KEY, String.valueOf(span.context().traceId())); // 向后传递traceid
                RpcContext.getContext().setAttachment(TRACE_ID_LONG_KEY, String.valueOf(span.context().traceId()));
            }
    
            String parentId = parentIdThreadLocal.get();
            if (StringUtils.isNotEmpty(parentId)) {
                resetFieldValue(span.context(), SPAN_CONTEXT_PARENT_FIELD_NAME, Long.parseLong(parentId));  // 重置parentId
            }
    
            // parentID下传
            RpcContext.getContext().getAttachments().put(PARENT_ID_LONG_KEY, String.valueOf(span.context().spanId()));
            
            result = invoker.invoke(invocation);
        } catch (Exception e) {
            System.out.println("【dubbo-filter】过滤器处理出现异常!!!");
            e.printStackTrace();
            span.error(e);
        } finally {
            setTags(invocation, result, span);
            span.finish();
        }
        
        return result;
    }
    
    @Override
    protected void setSpanKind(Span span) {
        span.kind(Span.Kind.CONSUMER);
        span.tag(TAG_KEY_TYPE, Span.Kind.CONSUMER.name());
    }
}
