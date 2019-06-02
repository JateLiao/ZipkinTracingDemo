package com.alibaba.apm.thirddubboprovider.service.impl;

import com.alibaba.apm.thirddubboprovider.service.ThirdDubboProvider;

/**
 * @author Administrator
 * @description: ${TODO}
 * @Createtime 2019/5/2217:17
 */
public class ThirdDubboProviderImpl implements ThirdDubboProvider {
    
    public String responseHello(String param) {
        return "第三方provider响应for param: " + param;
    }
}
