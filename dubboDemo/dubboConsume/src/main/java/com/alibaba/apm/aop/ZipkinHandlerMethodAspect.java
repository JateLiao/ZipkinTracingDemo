package com.alibaba.apm.aop;

import com.alibaba.apm.interceptor.ZipkinInterceptor;
import com.alibaba.apm.utils.JsonUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;

/**
 * @author liaoshijie
 * @description: ${TODO}
 * @Createtime 2019/6/4 10:21
 */
@Component
@Aspect
public class ZipkinHandlerMethodAspect {
    
    // com.alibaba.apm.controller.ConsumerController.getInfo
    //@Pointcut("execution(public * org.springframework.web.method.support.InvocableHandlerMethod.*(..))")
    @Pointcut("execution(public * com.alibaba.apm.controller.*.*(..))")
    public void pointCut() {
        System.out.println("大家好，我是pointcut");
    }
    
    //@AfterReturning(pointcut  = "pointCut()", returning = "obj")
    //public void afterReturning(Object obj) {
    //    System.out.println("AOP开始拦截controller返回值");
    //
    //    ZipkinInterceptor.getResponseValueThreadLocal().set(JsonUtils.toJsonWithJackson(obj));
    //}
    
    @Around("pointCut()")
    public Object afterReturning(ProceedingJoinPoint pjp) throws Throwable {
        Method method = null;
        String methodName = pjp.getSignature().getName();
        try {
            Method[] methods = pjp.getTarget().getClass().getDeclaredMethods();
            for (Method mtd : methods                 ) {
                if (!mtd.getName().equals(methodName)) {
                    continue;
                }
                method = mtd;
                break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        boolean isAopMethod = false;
        if (method != null) {
            PostMapping postMapping = method.getAnnotation(PostMapping.class);
            GetMapping getMapping = method.getAnnotation(GetMapping.class);
            RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
            
            if (null != postMapping || null != getMapping || null != requestMapping) {
                isAopMethod = true;
            }
        }
        
        Object value = pjp.proceed();
        
        if (isAopMethod) { // 只有特定方法的返回值才被放入ThreadLocal，作为日志记录来源
            ZipkinInterceptor.getResponseValueThreadLocal().set(JsonUtils.toJsonWithJackson(value));
        }
    
        return value;
    }
}
