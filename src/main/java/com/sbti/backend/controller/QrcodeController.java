package com.sbti.backend.controller;

import com.sbti.backend.service.WeChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class QrcodeController {

    private static final Logger log = LoggerFactory.getLogger(QrcodeController.class);

    private final WeChatService weChatService;

    public QrcodeController(WeChatService weChatService) {
        this.weChatService = weChatService;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "ok");
        result.put("service", "weixinspringboot");
        result.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(result);
    }

    @PostMapping(value = "/qrcode", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> generateQrCode(@RequestBody QrcodeRequest request) {
        Map<String, Object> result = new HashMap<>();

        try {
            String accessToken = weChatService.getAccessToken();

            byte[] imageData = weChatService.generateQRCode(
                accessToken,
                request.getPage(),
                request.getScene(),
                request.getWidth()
            );

            String base64Image = Base64.getEncoder().encodeToString(imageData);

            result.put("success", true);
            result.put("buffer", "data:image/png;base64," + base64Image);

            log.info("QRCode generated successfully for page={}", request.getPage());
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Failed to generate QRCode: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    public static class QrcodeRequest {
        private String page = "pages/index/index";
        private String scene = "index";
        private int width = 280;

        public String getPage() { return page; }
        public void setPage(String page) { this.page = page; }
        public String getScene() { return scene; }
        public void setScene(String scene) { this.scene = scene; }
        public int getWidth() { return width; }
        public void setWidth(int width) { this.width = width; }
    }
}
