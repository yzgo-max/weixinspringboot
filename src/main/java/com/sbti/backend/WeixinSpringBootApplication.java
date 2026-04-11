package com.sbti.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class WeixinSpringBootApplication {
    public static void main(String[] args) {
        SpringApplication.run(WeixinSpringBootApplication.class, args);
        System.out.println("========================================");
        System.out.println("  WeChat SpringBoot started on port 8080");
        System.out.println("  QRCode API: POST /api/qrcode");
        System.out.println("  Health:     GET /api/health");
        System.out.println("========================================");
    }
}
