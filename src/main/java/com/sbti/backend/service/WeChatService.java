package com.sbti.backend.service;

import com.sbti.backend.config.WeChatConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class WeChatService {

    private static final Logger log = LoggerFactory.getLogger(WeChatService.class);

    private final WeChatConfig weChatConfig;
    private final RestTemplate restTemplate = new RestTemplate();

    private String cachedAccessToken;
    private long tokenExpireTime = 0;

    public WeChatService(WeChatConfig weChatConfig) {
        this.weChatConfig = weChatConfig;
    }

    /**
     * Get access_token with caching (valid for ~2h from WeChat)
     */
    public synchronized String getAccessToken() {
        long now = System.currentTimeMillis();
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

            String token = extractJsonField(response, "access_token");
            if (token == null || token.isEmpty()) {
                log.error("Failed to get access_token: {}", response);
                throw new RuntimeException("获取access_token失败: " + response);
            }

            int expiresIn = Integer.parseInt(extractJsonField(response, "expires_in", "7200"));
            cachedAccessToken = token;
            tokenExpireTime = now + (expiresIn - 300) * 1000L;

            log.info("Access_token obtained successfully, expires in {}s", expiresIn);
            return cachedAccessToken;
        } catch (Exception e) {
            log.error("Error getting access_token", e);
            throw new RuntimeException("获取access_token异常: " + e.getMessage(), e);
        }
    }

    /**
     * Generate unlimited mini-program QR code
     */
    public byte[] generateQRCode(String accessToken, String page, String scene, int width) throws Exception {
        String url = "https://api.weixin.qq.com/wxa/getunlimited?access_token=" + accessToken;

        StringBuilder body = new StringBuilder("{");
        body.append("\"scene\":\"").append(scene != null ? scene : "index").append("\",");
        body.append("\"page\":\"").append(page != null ? page : "pages/index/index").append("\",");
        body.append("\"width\":").append(width > 0 ? width : 280).append(",");
        body.append("\"is_hyaline\":true");
        body.append("}");

        log.info("Generating QRCode: page={}, scene={}, width={}", page, scene, width);

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(body.toString(), headers);

        org.springframework.http.ResponseEntity<byte[]> response = restTemplate.exchange(
            url, org.springframework.http.HttpMethod.POST, entity, byte[].class
        );

        byte[] imageBytes = response.getBody();
        if (imageBytes == null || imageBytes.length == 0) {
            throw new RuntimeException("微信API返回空数据");
        }

        // Check error response
        if (imageBytes[0] == '{') {
            String errorMsg = new String(imageBytes, "UTF-8");
            log.error("WeChat API error: {}", errorMsg);
            throw new RuntimeException("微信小程序码生成失败: " + errorMsg);
        }

        log.info("QRCode generated successfully, size={} bytes", imageBytes.length);
        return imageBytes;
    }

    private String extractJsonField(String json, String key) {
        return extractJsonField(json, key, "");
    }

    private String extractJsonField(String json, String key, String defaultValue) {
        String searchKey = "\"" + key + "\":";
        int start = json.indexOf(searchKey);
        if (start < 0) return defaultValue;
        start += searchKey.length();
        if (start >= json.length()) return defaultValue;

        char ch = json.charAt(start);
        if (ch == '"') {
            int end = json.indexOf('"', start + 1);
            if (end < 0) return defaultValue;
            return json.substring(start + 1, end);
        } else {
            int end = start;
            while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) {
                end++;
            }
            return json.substring(start, end);
        }
    }
}
