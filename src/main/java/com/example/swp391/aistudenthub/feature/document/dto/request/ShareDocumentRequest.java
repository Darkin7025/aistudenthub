package com.example.swp391.aistudenthub.feature.document.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareDocumentRequest {
    @NotBlank(message = "Email người nhận không được để trống")
    @Email(message = "Email không hợp lệ")
    private String targetEmail;

    private String permission = "VIEW";
}
