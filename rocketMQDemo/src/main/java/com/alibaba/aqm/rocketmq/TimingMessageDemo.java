package com.alibaba.aqm.rocketmq;

import com.aliyun.openservices.ons.api.*;
import com.aliyun.openservices.shade.com.alibaba.fastjson.JSON;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * @author liaoshijie
 * @description: ${TODO}
 * @Createtime 2019/8/22 10:58
 */
public class TimingMessageDemo {
    
    /**
     * main method.
     * 定时消息测试.
     **/
    public static void main(String[] args) throws Exception {
        String accessKeyId = "LTAIeQDEnU1gAUBN";
        String accessSecretKey = "9AWhb04dn3XsHowj7983OgEoEC0QFU";
        String messageAddr = "http://MQ_INST_1160457043414751_Baox7fPg.mq-internet-access.mq-internet.aliyuncs.com:80";
        String topic = "order_unpaid_cancel_delay_topic_test";
        //topic = "delay_topic_test";
    
    
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.AccessKey, accessKeyId);
        properties.put(PropertyKeyConst.SecretKey, accessSecretKey);
        properties.put(PropertyKeyConst.NAMESRV_ADDR, messageAddr);
    
        Producer producer = ONSFactory.createProducer(properties);
        producer.start();
    
        Message msg = new Message();
        msg.setTopic(topic);
        msg.setTag("defaul_tag");
        msg.setBody("xxxxx".getBytes("UTF-8"));
        
        long delayTime = TimeUnit.MINUTES.toMillis(5); // 延时五分钟
        
        msg.setStartDeliverTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2019-08-22 13:56:00").getTime());
        SendResult sendResult = producer.send(msg);
        
        System.out.println("投递结果：" + JSON.toJSONString(sendResult));
    }
}
