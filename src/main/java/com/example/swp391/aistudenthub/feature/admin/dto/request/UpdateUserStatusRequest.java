package com.example.swp391.aistudenthub.feature.admin.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO để kích hoạt hoặc vô hiệu hóa tài khoản user.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserStatusRequest {

    @NotNull(message = "Trạng thái active không được để trống")
    private Boolean active;
}
