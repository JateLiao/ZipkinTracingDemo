package com.alibaba.apm.interceptor;

import brave.Span;
import com.alibaba.apm.CommonZipkinHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

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
@Component("zipkinInterceptor")
public class ZipkinInterceptor extends CommonZipkinHandler implements HandlerInterceptor {
    @Autowired(
            required = false
    )
    //HandlerParser handlerParser = new HandlerParser();
    
    ZipkinInterceptor() {
    }
    
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object o) {
        //SpanCustomizer span = (SpanCustomizer)request.getAttribute(SpanCustomizer.class.getName());
        //if (span != null) {
        //    this.handlerParser.preHandle(request, o, span);
        //}
        
        /****************************************************************************/
        Span span = buildSpanFromTracing(request.getRequestURL().toString(), getTracing());
        traceIdThreadLocal.set(String.valueOf(span.context().traceId()));
        
        return true;
    }
    
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        //SpanCustomizer span = (SpanCustomizer)request.getAttribute(SpanCustomizer.class.getName());
        //if (span != null) {
        //    SpanCustomizingHandlerInterceptor.setHttpRouteAttribute(request);
        //}
        
        /****************************************************************************/
        
    }
    
    @Override
    protected void setSpanKind(Span span) {
        span.kind(Span.Kind.SERVER); // 针对SpringBoot应用，设置Kind为Server
    }
}
