package com.alibaba.apm;




import com.alibaba.apm.thirddubboprovider.service.ThirdDubboProvider;
//import com.alibaba.dubbo.config.annotation.Reference;
//import com.alibaba.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service("demoService")
public class DemoServiceImpl implements DemoService {


    @Autowired
    private ThirdDubboProvider thirdDubboProvider;
    StringBuffer stringBuffer = new StringBuffer();
    
    public String sayHello(String name) {
        
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 2; i++) {
            builder.append(thirdDubboProvider.responseHello(name)).append(", ");
        }
        ExecutorService service = Executors.newFixedThreadPool(5);
    
        //for (int i = 0; i < 1; i++) {
        //    service.submit(() -> {
        //        stringBuffer.append(thirdDubboProvider.responseHello(name)).append(", ");
        //    });
        //}
        service.shutdown();
        while (!service.isTerminated()) {
            try {
                Thread.sleep(100);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        //String thirdResult = thirdDubboProvider.responseHello(name);
        System.out.println("[" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "] Hello " + builder.toString() + ", request from consumer: " + RpcContext.getContext().getRemoteAddress());
        return "Hello " + stringBuffer.toString() + ", 加点中文区分区分，地址: " + RpcContext.getContext().getLocalAddress();
    }

}
