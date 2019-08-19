package com.alibaba.zipkinDemo.zipkintrace.zipkin;

import com.alibaba.zipkinDemo.zipkintrace.config.AliLogConfig;
import com.alibaba.zipkinDemo.zipkintrace.config.LogType;
import com.alibaba.zipkinDemo.zipkintrace.util.JsonUtils;
import com.aliyun.openservices.aliyun.log.producer.LogProducer;
import com.aliyun.openservices.aliyun.log.producer.ProducerConfig;
import com.aliyun.openservices.aliyun.log.producer.ProjectConfig;
import com.aliyun.openservices.log.common.LogContent;
import com.aliyun.openservices.log.common.LogItem;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Result;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.InetAddress;
import java.util.*;

/**
 * @author liaoshijie
 * @description: ${TODO}
 * @Createtime 2019/8/15 17:44
 */
@Component
@Slf4j
public class LogHelper implements InitializingBean {
    @Autowired
    private AliLogConfig aliLogConfig;
    
    private volatile LogProducer logProducer;
    
    @Override
    public void afterPropertiesSet() throws Exception {
        // 初始化LogProducer
        ProducerConfig producerConfig = new ProducerConfig();
        producerConfig.setBatchSizeThresholdInBytes(10 * 1024 * 1024);
        producerConfig.setBatchCountThreshold(40960);
        //producerConfig.setIoThreadCount(ioThreadCount);
        //producerConfig.setTotalSizeInBytes(totalSizeInBytes);
        logProducer = new LogProducer(producerConfig);
        logProducer.putProjectConfig(new ProjectConfig(aliLogConfig.getLogProject(),
                aliLogConfig.getLogEndpoint(), aliLogConfig.getAccessKeyId(), aliLogConfig.getAccessKeySecret()));
    }
    
    /**
     * 记录http请求日志
     * @param request req
     * @param response res
     * @param ex ex
     * @param responseValue responsevalue
     * @param traceID  traceID
     */
    public void writeHttpLog(HttpServletRequest request, HttpServletResponse response, Exception ex, String responseValue, String traceID) {
        try {
            Map<String, String> map = new HashMap<>();
            map.put("LogType", LogType.HTTP.name());
            map.put("RequestUrl", request.getRequestURL().toString());
            map.put("RequestMethod", request.getMethod());
            map.put("RequestBody", JsonUtils.toJsonWithJackson(request.getParameterMap()));
            map.put("ResponseBody", responseValue);
            
            Date date = new Date();
            map.put("LogTimeL", String.valueOf(date.getTime()));
            map.put("LogTime", DateFormatUtils.format(date, "yyyy-MM-dd HH:mm:ss.SSS"));
            
            map.put("ExceptionTraceInfo", ex != null ? ExceptionUtils.getStackTrace(ex) : "");
            map.put("ClientIP", request.getRemoteAddr());
            map.put("ServerIP", InetAddress.getLocalHost().getHostAddress());
            map.put("TraceID", traceID);
            map.put("ResponseStatusCode", String.valueOf(response.getStatus()));
            
            // 请求header
            Enumeration<String> requestEnumeration = request.getHeaderNames();
            if (null != requestEnumeration) {
                while (requestEnumeration.hasMoreElements()) {
                    String key = requestEnumeration.nextElement();
                    String value = request.getHeader(key);
                    map.put("RequestHeader_" + firstCharToUpper(key), value);
                }
            }
            
            // 响应header
            Collection<String> responseHeaders = response.getHeaderNames();
            if (null != responseHeaders && !responseHeaders.isEmpty()) {
                responseHeaders.stream().forEach(headerName -> {
                    map.put("ResponseHeader_" + firstCharToUpper(headerName), response.getHeader(headerName));
                });
            }
            
            if (!map.isEmpty()) {
                LogItem logItem = new LogItem();
                ArrayList<LogContent> contents = new ArrayList<>(map.size());
                map.entrySet().stream().forEach(entry -> contents.add(new LogContent(entry.getKey(), entry.getValue())));
                logItem.SetLogContents(contents);
                logProducer.send(aliLogConfig.getLogProject(), aliLogConfig.getServerLogstore(), logItem);
            }
        } catch (Exception e) {
            log.error("[Zipkin]记录http请求日志异常", e);
        }
    }
    
    /**
     * 记录WebService求日志
     */
    public void writeWebServiceLog(Invocation invocation, Result result, String type) {
        try {
            Map<String, String> logMap = new HashMap<>();
            logMap.put("LogType", LogType.DUBBO.name());
            logMap.put("TraceID", CommonZipkinHandler.traceIdThreadLocal.get());
            logMap.put("InterfaceName", invocation.getInvoker().getInterface().getName());
            logMap.put("Method", invocation.getMethodName());
            logMap.put("RequestBody", JsonUtils.toJsonWithJacksonYMDHms(invocation.getArguments()));
            logMap.put("ExceptionTraceInfo", null != result && null != result.getException() ? ExceptionUtils.getStackTrace(result.getException()) : "");
            logMap.put("ResponseBody", result != null && result.getValue() != null ? JsonUtils.toJsonWithJacksonYMDHms(result.getValue()) : "");
            logMap.put("ServiceType", type);
            //logMap.put("clientIP", "xxxxxxxxx");
            //logMap.put("serverIP", "xxxxxxxxx");
            
            Date date = new Date();
            logMap.put("LogTime", DateFormatUtils.format(date, "yyyy-MM-dd HH:mm:ss.SSS"));
            logMap.put("LogTimeL", String.valueOf(date.getTime()));
            
            String spanName = invocation.getInvoker().getInterface().getName() + "/" + invocation.getMethodName();
            logMap.put("WholeMethod", spanName);
            
            LogItem logItem = new LogItem();
            ArrayList<LogContent> contents = new ArrayList<>(logMap.size());
            logMap.entrySet().stream().forEach(entry -> contents.add(new LogContent(entry.getKey(), entry.getValue())));
            logItem.SetLogContents(contents);
            logProducer.send(aliLogConfig.getLogProject(), aliLogConfig.getServerLogstore(), logItem);
        } catch (Exception e) {
            log.error("[Zipkin]记录WebService调用日志异常", e);
        }
    }
    
    /**
     * TODO 首字母转大写.
     *
     * @param str
     *            .
     * @return .
     */
    public static String firstCharToUpper(String str) {
        char[] arr = str.toCharArray();
        if (arr.length > 0 && 97 <= (int) arr[0] && (int) arr[0] <= 122) {
            arr[0] = (char) ((int) arr[0] - 32);
        } else {
            return str;
        }
        
        return new String(arr);
    }
}
