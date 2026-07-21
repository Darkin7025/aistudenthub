package com.example.swp391.aistudenthub.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.payos.PayOS;

import org.springframework.util.StringUtils;

@Slf4j
@Configuration
public class PayOSConfig {

    @Value("${payos.client-id:}")
    private String clientId;

    @Value("${payos.api-key:}")
    private String apiKey;

    @Value("${payos.checksum-key:}")
    private String checksumKey;

    @Bean
    public PayOS payOS() {
        String cleanClientId = StringUtils.hasText(clientId) ? clientId.trim() : "";
        String cleanApiKey = StringUtils.hasText(apiKey) ? apiKey.trim() : "";
        String cleanChecksumKey = StringUtils.hasText(checksumKey) ? checksumKey.trim() : "";

        log.info("PayOS Client Loaded -> ClientId: [{}], ApiKey len={}, ChecksumKey len={}",
                cleanClientId.length() > 5 ? cleanClientId.substring(0, 5) + "***" : cleanClientId,
                cleanApiKey.length(),
                cleanChecksumKey.length());

        return new PayOS(cleanClientId, cleanApiKey, cleanChecksumKey);
    }
}
