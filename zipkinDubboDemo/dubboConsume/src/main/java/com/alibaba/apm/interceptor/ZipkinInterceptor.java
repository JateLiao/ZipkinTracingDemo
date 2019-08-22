package com.alibaba.apm.interceptor;

import brave.Span;
import com.alibaba.apm.utils.JsonUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @Title: ${FILE_NAME}
 * @Company: com.lsj
 * @Package com.alibaba.apm.interceptor
 * @Description: ${TODO}
 * @Author liao
 * @Createtine 2019/6/2 21:21
 */
public class ZipkinInterceptor extends AbstractZipkinInterceptor {
    
    private static InheritableThreadLocal<Span> spanThreadLocal = new InheritableThreadLocal<>();
    private static InheritableThreadLocal<String> responseValueThreadLocal = new InheritableThreadLocal<>();
    
    /**
     * 获取字段值： responseValueThreadLocal.
     *
     * @return 返回字段值： responseValueThreadLocal.
     */
    public static ThreadLocal<String> getResponseValueThreadLocal() {
        return responseValueThreadLocal;
    }
    
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object o) {
        Span span = null;
        try {
            //span = buildSpanFromTracing(request.getRequestURI().toString(), getTracing());
            span = buildSpanFromTracingAli(request.getRequestURI().toString(), getTracing());
            if (span.context().parentId() != null) {
                Span newSpan = buildNewTraceSpanFromTracing(request.getRequestURI().toString(), getTracing());
                resetFieldValue(span.context(), SPAN_CONTEXT_TRACEID_FIELD_NAME, newSpan.context().traceId());
                resetFieldValue(span.context(), SPAN_CONTEXT_PARENT_FIELD_NAME, 0L);
            }
            span.start(System.currentTimeMillis());
            
            System.out.println("【dubbo-filter】preHandle: " + span.context().traceId() + ", "
                    + span.context().parentId() + ", "
                    + span.context().spanId() + ", "
                    + Thread.currentThread().getId() + ", "
                    + this + ", "
                    + span.hashCode());
            
            setSpanKind(span);
            setSpanTag(span, TAG_KEY_PARAM, JsonUtils.toJsonWithJackson(request.getParameterMap()));
            setSpanTag(span, TAG_KEY_WHOLE_URL, request.getRequestURL().toString());
            setSpanTag(span, TAG_KEY_SPANID, String.valueOf(span.context().spanId()));
            setSpanTag(span, TAG_KEY_PARENTID, String.valueOf(span.context().parentId()));
            setSpanTag(span, TAG_KEY_THREADID, String.valueOf(Thread.currentThread().getId()));
            if (o != null && o instanceof HandlerMethod) {
                span.tag(TAG_KEY_METHOD, ((HandlerMethod)o).getMethod().getDeclaringClass().getName() + "/" + ((HandlerMethod)o).getMethod().getName());
            }
    
    
            //Long parentId = span.context().parentId();
            //if (parentId == null || parentId == 0L) {
            //    parentId = span.context().traceId();
            //}
            Long parentId = span.context().spanId();
            
            traceIdThreadLocal.set(String.valueOf(span.context().traceId())); // traceId向后传递，这里的traceId将作为整个链路的起点，优先级最高
            parentIdThreadLocal.set(String.valueOf(parentId)); // api接口优先级一般最高，在此设置
            spanThreadLocal.set(span);
        } catch (Exception e) {
            e.printStackTrace();
            if (span != null) {
                span.tag("pre-handle-error", e.getMessage());
            }
        }
        
        return true;
    }
    
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
    }
    
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        Span span = spanThreadLocal.get();
        if (null == span) {
            return;
        }
        
        try {
            String value = responseValueThreadLocal.get();
            setSpanTag(span, TAG_KEY_RESULT, value);
            span.tag(TAG_KEY_RESULT, value);
            if (null != ex) {
                setSpanTag(span, TAG_KEY_AFTER_ERROR, ex.getMessage());
            }
        } catch (Exception e) {
            span.error(e);
        } finally {
            //span.finish();
            doSpanFinish(span);
            
            // 线程变量清除
            traceIdThreadLocal.remove();
            parentIdThreadLocal.remove();
            spanThreadLocal.remove();
            responseValueThreadLocal.remove();
        }
    }
    
    @Override
    protected void setSpanKind(Span span) {
        span.kind(Span.Kind.SERVER); // 针对SpringBoot应用，设置Kind为Server
        span.tag(TAG_KEY_TYPE, String.valueOf(Span.Kind.SERVER));
    }
}
