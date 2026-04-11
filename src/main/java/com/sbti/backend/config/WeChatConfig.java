package com.sbti.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WeChatConfig {

    @Value("${wechat.appid}")
    private String appId;

    @Value("${wechat.appsecret}")
    private String appSecret;

    @Value("${wechat.token.cache-seconds:7000}")
    private int tokenCacheSeconds;

    public String getAppId() { return appId; }
    public String getAppSecret() { return appSecret; }
    public int getTokenCacheSeconds() { return tokenCacheSeconds; }
}
