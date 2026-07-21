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
        String cleanClientId = clientId != null ? clientId.trim() : "";
        String cleanApiKey = apiKey != null ? apiKey.trim() : "";
        String cleanChecksumKey = checksumKey != null ? checksumKey.trim() : "";

        log.info("PayOS Bean Init -> ClientId: [{}] (len={}), ApiKey: [{}] (len={}), ChecksumKey: [{}] (len={})",
                cleanClientId.length() > 5 ? cleanClientId.substring(0, 5) + "***" : cleanClientId, cleanClientId.length(),
                cleanApiKey.length() > 5 ? cleanApiKey.substring(0, 5) + "***" : cleanApiKey, cleanApiKey.length(),
                cleanChecksumKey.length() > 5 ? cleanChecksumKey.substring(0, 5) + "***" : cleanChecksumKey, cleanChecksumKey.length());

        return new PayOS(cleanClientId, cleanApiKey, cleanChecksumKey);
    }
}
