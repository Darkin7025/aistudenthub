package com.example.swp391.aistudenthub.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.payos.PayOS;

@Slf4j
@Configuration
public class PayOSConfig {

    @Value("${PAYOS_CLIENT_ID:${payos.client-id:}}")
    private String clientId;

    @Value("${PAYOS_API_KEY:${payos.api-key:}}")
    private String apiKey;

    @Value("${PAYOS_CHECKSUM_KEY:${payos.checksum-key:}}")
    private String checksumKey;

    @Bean
    public PayOS payOS() {
        log.info("Initializing PayOS client with clientId: {}...", 
                (clientId != null && clientId.length() > 5) ? clientId.substring(0, 5) + "***" : "EMPTY");
        return new PayOS(clientId, apiKey, checksumKey);
    }
}
