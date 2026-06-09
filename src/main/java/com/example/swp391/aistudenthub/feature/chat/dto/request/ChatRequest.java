package com.example.swp391.aistudenthub.feature.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;

@Data
public class ChatRequest {
    @NotBlank(message = "Nội dung chat không được để trống")
    private String message;
    
    private UUID sessionId;
}
