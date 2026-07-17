package com.example.swp391.aistudenthub.feature.admin.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO để cập nhật hàng loạt cấu hình hệ thống.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSystemConfigRequest {

    @NotEmpty(message = "Danh sách cấu hình không được để trống")
    @Valid
    private List<ConfigEntry> configs;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfigEntry {

        @NotBlank(message = "Config key không được để trống")
        @Size(max = 100, message = "Config key tối đa 100 ký tự")
        private String configKey;

        @NotBlank(message = "Config value không được để trống")
        @Size(max = 2000, message = "Config value tối đa 2000 ký tự")
        private String configValue;
    }
}
