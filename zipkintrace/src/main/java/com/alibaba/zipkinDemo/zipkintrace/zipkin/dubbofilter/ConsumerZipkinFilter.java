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
@Activate(group = CommonConstants.CONSUMER)
public class ConsumerZipkinFilter extends AbstractZipkinFilter {
    
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
            String traceIdInvoke = traceIdThreadLocal.get(); // traceId优先从http api接口获取，因为优先级最高
            if (StringUtils.isEmpty(traceIdInvoke)) { // 此处为空，则说明链路入口不是http接口，可能是一个dubbo的provider
                traceIdInvoke = RpcContext.getContext().getAttachments().get(TRACE_ID_LONG_KEY);
            } else { // 此处不为空，那么链路入口就是http接口，后面的链路都用此traceId
                RpcContext.getContext().getAttachments().put(TRACE_ID_LONG_KEY, traceIdInvoke); // 向后传递traceid
                if (!traceIdInvoke.equals(currentTraceIdL)) {
                    resetFieldValue(span.context(), SPAN_CONTEXT_TRACEID_FIELD_NAME, Long.parseLong(traceIdInvoke));  // 重置traceId
                }
            }
    
            currentTraceIdL = String.valueOf(span.context().traceId());
            if (StringUtils.isEmpty(traceIdInvoke) || !traceIdInvoke.equals(currentTraceIdL)) { // 为空一般表示链路起点，和之前已存在的traceId不一致则需要重置
                RpcContext.getContext().getAttachments().put(TRACE_ID_LONG_KEY, String.valueOf(span.context().traceId())); // 向后传递traceid
            }
            
            String parentId = parentIdThreadLocal.get();
            if (StringUtils.isNotEmpty(parentId)) { // 已存在parentId，说明当前服务既是provider，同时也是consumer
                resetFieldValue(span.context(), SPAN_CONTEXT_PARENT_FIELD_NAME, Long.parseLong(parentId));  // 重置parentId
            } else { // 不存在parentId，说明当前服务为consumer，并且并行调用其他服务
                resetFieldValue(span.context(), SPAN_CONTEXT_PARENT_FIELD_NAME, 0L);  // 重置parentId
            }
            
            // parentID下传
            RpcContext.getContext().getAttachments().put(PARENT_ID_LONG_KEY, String.valueOf(span.context().spanId()));
        } catch (Exception e) {
            log.error("【dubbo-filter】过滤器处理出现异常!!!", e);
            span.error(e);
        }
    
        Result result = null;
        try {
            // 单独处理正常的业务请求，避免因为Filter的异常影响正常的业务调用
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
            writeDubboLog(invocation, result, Span.Kind.CONSUMER.name());
            
            setTags(invocation, result, span);   //span.finish();
            doSpanFinish(span);
        }
        
        return result;
    }
    
    @Override
    protected void setSpanKind(Span span) {
        span.kind(Span.Kind.CONSUMER);
        span.tag(TAG_KEY_TYPE, Span.Kind.CONSUMER.name());
    }
}
