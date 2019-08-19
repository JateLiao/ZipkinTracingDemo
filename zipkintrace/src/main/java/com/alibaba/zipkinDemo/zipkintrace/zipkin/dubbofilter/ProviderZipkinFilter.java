package com.alibaba.zipkinDemo.zipkintrace.zipkin.dubbofilter;

import brave.Span;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;

/**
 * @author liaoshijie
 * @description: ${TODO}
 * @Createtime 2019/5/24 16:17
 */
@Slf4j
@Activate(group = CommonConstants.PROVIDER)
public class ProviderZipkinFilter extends AbstractZipkinFilter {
    
    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        log.info("【dubbo-filter】方法名: " + invocation.getMethodName());
        
        if (isEcho(invocation)) { // 过滤dubbo回声检测
            return new AsyncRpcResult(invocation);
        }
    
        // 构造span
        brave.Span span = buildSpanFromTracing(invocation);
        if (null == span) {
            log.error("【dubbo-filter】Span构造失败");
            throw new RpcException("【dubbo-filter】Span构造失败");
        }
        
        try {
            String currentTraceIdL = String.valueOf(span.context().traceId());
            String traceIdInvoke = RpcContext.getContext().getAttachments().get(TRACE_ID_LONG_KEY);
            
            // 当前traceid和接收到的traceid不一样，则需要重置，为了形成链路
            if ((StringUtils.isEmpty(traceIdInvoke) || !traceIdInvoke.equals(currentTraceIdL)) && StringUtils.isNotEmpty(traceIdInvoke)) {
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
        } catch (Exception e) {
            log.error("【dubbo-filter】过滤器处理出现异常!!!", e);
            span.error(e);
        }
    
        Result result = null;
        try {
            // 单独处理正常的业务请求，避免因为Filter影响正常的业务调用
            result = invoker.invoke(invocation);
        } catch (RpcException re) {
            log.error("【dubbo-filter】过滤器处理出现异常!!!", re);
            span.error(re);
            throw re;
        } catch (Exception e) {
            log.error("【dubbo-filter】过滤器处理出现异常!!!", e);
            span.error(e);
        } finally {
            // 日志上报
            writeDubboLog(invocation, result, Span.Kind.PRODUCER.name());
            
            setTags(invocation, result, span);   //span.finish();
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
