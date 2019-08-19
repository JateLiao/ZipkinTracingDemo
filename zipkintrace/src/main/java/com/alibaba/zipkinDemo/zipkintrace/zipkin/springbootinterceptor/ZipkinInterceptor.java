package com.alibaba.zipkinDemo.zipkintrace.zipkin.springbootinterceptor;

import brave.Span;
import com.alibaba.zipkinDemo.zipkintrace.util.JsonUtils;
import com.alibaba.zipkinDemo.zipkintrace.zipkin.LogHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author liaoshijie
 * @description: ${TODO}
 * @Createtime 2019/6/3 18:35
 */
@Component
public class ZipkinInterceptor extends AbstractZipkinInterceptor {
    
    private static InheritableThreadLocal<Span> spanThreadLocal = new InheritableThreadLocal<>();
    private static InheritableThreadLocal<String> responseValueThreadLocal = new InheritableThreadLocal<>();
    private static InheritableThreadLocal<Exception> exceptionThreadLocal = new InheritableThreadLocal<>();
    
    @Autowired
    private LogHelper logHelper;
    
    /**
     * 获取字段值： responseValueThreadLocal.
     *
     * @return 返回字段值： responseValueThreadLocal.
     */
    public static ThreadLocal<String> getResponseValueThreadLocal() {
        return responseValueThreadLocal;
    }
    
    /**
     * 获取字段值： exceptionThreadLocal.
     *
     * @return 返回字段值： exceptionThreadLocal.
     */
    public static InheritableThreadLocal<Exception> getExceptionThreadLocal() {
        return exceptionThreadLocal;
    }
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Span span = null;
        try {
            String spanName = request.getRequestURI().toString();
            span = buildSpanFromTracing(spanName, getTracing(), false);
            if (span.context().parentId() != null) { // 同一线程，线程id相同，traceId相同，并且已存在parenId，不符合链路起点特征，需要重置
                Span newSpan = buildSpanFromTracing(spanName, getTracing(), true);
                resetFieldValue(span.context(), SPAN_CONTEXT_TRACEID_FIELD_NAME, newSpan.context().traceId());
                resetFieldValue(span.context(), SPAN_CONTEXT_PARENT_FIELD_NAME, 0L);
            }
            setSpanKind(span);
            setSpanTag(span, TAG_KEY_PARAM, JsonUtils.toJsonWithJackson(request.getParameterMap()));
            setSpanTag(span, TAG_KEY_WHOLE_URL, request.getRequestURL().toString());
            setSpanTag(span, TAG_KEY_SPANID, String.valueOf(span.context().spanId()));
            setSpanTag(span, TAG_KEY_PARENTID, String.valueOf(span.context().parentId()));
            setSpanTag(span, TAG_KEY_THREADID, String.valueOf(Thread.currentThread().getId()));
            
            if (handler != null && handler instanceof HandlerMethod) {
                setSpanTag(span, TAG_KEY_METHOD, ((HandlerMethod) handler).getMethod().getDeclaringClass().getName() + "/" + ((HandlerMethod) handler).getMethod().getName());
            }
            
            //parentId = span.context().parentId();
            //if (parentId == null || parentId == 0L) {
            //    parentId = span.context().traceId();
            //} else {
            //    parentId = span.context().spanId();
            //}
            
            traceIdThreadLocal.set(String.valueOf(span.context().traceId())); // traceId向后传递，这里的traceId将作为整个链路的起点，优先级最高
            parentIdThreadLocal.set(String.valueOf(span.context().spanId())); // api接口优先级一般最高，在此设置
            spanThreadLocal.set(span);
        } catch (Exception e) {
            e.printStackTrace();
            setSpanTag(span, TAG_KEY_PRE_ERROR, e.getMessage());
        }
        
        return true;
    }
    
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        Span span = spanThreadLocal.get();
        if (null == span) {
            return;
        }
        
        try {
            String value = responseValueThreadLocal.get();
            setSpanTag(span, TAG_KEY_RESULT, value);
            if (null != ex) {
                setSpanTag(span, TAG_KEY_AFTER_ERROR, ex.getMessage());
            }
        } catch (Exception e) {
            span.error(e);
        } finally {
            doSpanFinish(span);
            
            // 日志处理
            String traceIdString = span.context().traceIdString();
            addTraceIdHeader(response, traceIdString); // 响应header增加tradeId
            writeHttpLog(request, response, traceIdString); // 记录请求日志
            
            // 线程变量清除
            traceIdThreadLocal.remove();
            parentIdThreadLocal.remove();
            spanThreadLocal.remove();
            responseValueThreadLocal.remove();
            exceptionThreadLocal.remove();
        }
    }
    
    /**
     * 记录请求日志
     * @param request 请求
     * @param response 响应
     * @param traceIdString traceID
     */
    private void writeHttpLog(HttpServletRequest request, HttpServletResponse response, String traceIdString) {
        logHelper.writeHttpLog(request, response, exceptionThreadLocal.get(), responseValueThreadLocal.get(), traceIdString);
    }
    
    /**
     * 响应header增加tradeId
     * @param response response
     * @param traceIdString traceIdString
     */
    private void addTraceIdHeader(HttpServletResponse response, String traceIdString) {
        if (null == response) {
            return;
        }
        response.addHeader("TraceID", traceIdString);
    }
    
    @Override
    protected void setSpanKind(Span span) {
        span.kind(Span.Kind.SERVER);
        span.tag(TAG_KEY_TYPE, String.valueOf(Span.Kind.SERVER));
    }
}
