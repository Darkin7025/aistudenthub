package com.example.swp391.aistudenthub.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO dùng chung để trả về thông báo văn bản đơn giản.
 * Thường được wrap bên trong {@link ApiResponse}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {
    private String message;
}
