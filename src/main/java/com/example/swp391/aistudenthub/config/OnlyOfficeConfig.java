package com.example.swp391.aistudenthub.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Getter
@Setter
@Component
public class OnlyOfficeConfig {

    @Value("${onlyoffice.docservice.url:http://localhost:8000}")
    private String docserviceUrl;

    @Value("${onlyoffice.docservice.secret:onlyoffice_super_secret_jwt_key_2026_32bytes}")
    private String docserviceSecret;

    @Value("${onlyoffice.docservice.header:Authorization}")
    private String docserviceHeader;

    @Value("${onlyoffice.docservice.enabled:true}")
    private boolean enabled;

    public String createToken(Map<String, Object> payload) {
        if (docserviceSecret == null || docserviceSecret.trim().isEmpty()) {
            return null;
        }
        byte[] rawBytes = docserviceSecret.getBytes(StandardCharsets.UTF_8);
        byte[] keyBytes = rawBytes;
        if (rawBytes.length < 32) {
            keyBytes = new byte[32];
            System.arraycopy(rawBytes, 0, keyBytes, 0, rawBytes.length);
        }
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);
        return Jwts.builder()
                .claims(payload)
                .signWith(key)
                .compact();
    }
}
