package com.sbti.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

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

    /**
     * RestTemplate with timeout configuration
     * Connect: 5s, Read: 10s
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        builder.setConnectTimeout(java.time.Duration.ofSeconds(5));
        builder.setReadTimeout(java.time.Duration.ofSeconds(10));
        return builder.build();
    }
}
