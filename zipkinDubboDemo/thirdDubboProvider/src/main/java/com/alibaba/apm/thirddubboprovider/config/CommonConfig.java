package com.alibaba.apm.thirddubboprovider.config;

import com.alibaba.zipkinDemo.zipkintrace.config.AliLogConfig;
import com.alibaba.zipkinDemo.zipkintrace.config.ZipkinConfig;
import com.alibaba.zipkinDemo.zipkintrace.statics.BeanStatics;
import com.alibaba.zipkinDemo.zipkintrace.zipkin.LogHelper;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Component;

/**
 * @author liaoshijie
 * @description: ${TODO}
 * @Createtime 2019/8/23 14:58
 */
@Component
public class CommonConfig {

    static {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"META-INF/spring/dubbo-demo-third-provider.xml"});
        context.start();
        BeanStatics.logHelper = context.getBean(LogHelper.class);
        BeanStatics.aliLogConfig = context.getBean(AliLogConfig.class);
        BeanStatics.zipkinConfig = context.getBean(ZipkinConfig.class);
    }
}
