package com.example.swp391.aistudenthub.feature.auth.dto;

import lombok.Data;

@Data
public class LogoutRequest {
    private String refreshToken;
}
