package com.example.swp391.aistudenthub.feature.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;

@Data
public class DocumentChatRequest {
    @NotBlank(message = "Câu hỏi không được để trống")
    private String question;
    
    private UUID sessionId;
}
