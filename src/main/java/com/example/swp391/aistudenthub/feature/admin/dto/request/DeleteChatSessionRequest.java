package com.example.swp391.aistudenthub.feature.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Records why an administrator removed a conversation for moderation purposes. */
@Data
public class DeleteChatSessionRequest {

    @NotBlank(message = "Lý do xóa phiên chat không được để trống")
    @Size(max = 500, message = "Lý do xóa phiên chat tối đa 500 ký tự")
    private String reason;
}
