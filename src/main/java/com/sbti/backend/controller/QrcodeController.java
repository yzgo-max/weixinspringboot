package com.sbti.backend.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class QrcodeController {

    private static final Logger log = LoggerFactory.getLogger(QrcodeController.class);

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "ok");
        result.put("service", "weixinspringboot");
        result.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(result);
    }

    @PostMapping(value = "/qrcode", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> generateQrCode(@Valid @RequestBody QrcodeRequest request) {
        try {
            ClassPathResource imageResource = new ClassPathResource("static/images/qrcode.png");
            try (InputStream is = imageResource.getInputStream()) {
                byte[] imageBytes = is.readAllBytes();
                log.info("QRCode static image returned for page={}, scene={}", request.getPage(), request.getScene());
                return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(imageBytes);
            }
        } catch (Exception e) {
            log.error("Failed to read QRCode image: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Data
    @Getter
    @Setter
    public static class QrcodeRequest {
        private String page = "pages/index/index";
        
        @NotBlank(message = "scene 不能为空")
        private String scene = "index";
        
        @Min(280)
        @Max(1280)
        private int width = 280;
        
        // Explicit getters for safety
        public String getPage() { return page; }
        public void setPage(String page) { this.page = page; }
        public String getScene() { return scene; }
        public void setScene(String scene) { this.scene = scene; }
        public int getWidth() { return width; }
        public void setWidth(int width) { this.width = width; }
    }
}
