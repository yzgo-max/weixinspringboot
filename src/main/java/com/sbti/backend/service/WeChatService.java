package com.sbti.backend.service;

import com.sbti.backend.config.WeChatConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@Service
public class WeChatService {

    private static final Logger log = LoggerFactory.getLogger(WeChatService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final WeChatConfig weChatConfig;
    private final RestTemplate restTemplate;

    private volatile String cachedAccessToken;
    private volatile long tokenExpireTime = 0;

    public WeChatService(WeChatConfig weChatConfig, RestTemplate restTemplate) {
        this.weChatConfig = weChatConfig;
        this.restTemplate = restTemplate;
    }

    /**
     * Get access_token with caching (valid for ~2h from WeChat)
     */
    public String getAccessToken() {
        long now = System.currentTimeMillis();
        if (cachedAccessToken != null && now < tokenExpireTime) {
            return cachedAccessToken;
        }

        synchronized (this) {
            // Double-check after acquiring lock
            if (cachedAccessToken != null && now < tokenExpireTime) {
                return cachedAccessToken;
            }

            String url = String.format(
                "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=%s&secret=%s",
                weChatConfig.getAppId(),
                weChatConfig.getAppSecret()
            );

            log.info("Requesting new access_token...");
            try {
                String response = restTemplate.getForObject(url, String.class);

                JsonNode json = objectMapper.readTree(response);
                String token = json.path("access_token").asText();
                if (token == null || token.isEmpty()) {
                    log.error("Failed to get access_token: {}", response);
                    throw new RuntimeException("获取access_token失败: " + response);
                }

                int expiresIn = json.path("expires_in").asInt(7200);
                cachedAccessToken = token;
                tokenExpireTime = now + (expiresIn - 300) * 1000L;

                log.info("Access_token obtained successfully, expires in {}s", expiresIn);
                return cachedAccessToken;
            } catch (Exception e) {
                log.error("Error getting access_token", e);
                throw new RuntimeException("获取access_token异常: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Generate unlimited mini-program QR code
     */
    public byte[] generateQRCode(String accessToken, String page, String scene, int width) throws Exception {
        String url = "https://api.weixin.qq.com/wxa/getunlimited?access_token=" + accessToken;

        // Build request body using Jackson for proper JSON encoding
        ObjectMapper bodyMapper = new ObjectMapper();
        ObjectNode body = bodyMapper.createObjectNode();
        body.put("scene", scene != null ? scene : "index");
        body.put("page", page != null ? page : "pages/index/index");
        body.put("width", Math.max(width, 280));
        body.put("is_hyaline", true);

        log.info("Generating QRCode: page={}, scene={}, width={}", page, scene, width);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(bodyMapper.writeValueAsString(body), headers);

        ResponseEntity<byte[]> response = restTemplate.exchange(
            url, HttpMethod.POST, entity, byte[].class
        );

        byte[] imageBytes = response.getBody();
        if (imageBytes == null || imageBytes.length == 0) {
            throw new RuntimeException("微信API返回空数据");
        }

        // Check error response (WeChat API returns JSON error starting with '{')
        if (imageBytes[0] == '{') {
            String errorMsg = new String(imageBytes, "UTF-8");
            log.error("WeChat API error: {}", errorMsg);
            throw new RuntimeException("微信小程序码生成失败: " + errorMsg);
        }

        log.info("QRCode generated successfully, size={} bytes", imageBytes.length);
        return imageBytes;
    }
}
