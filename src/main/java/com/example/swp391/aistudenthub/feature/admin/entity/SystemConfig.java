package com.example.swp391.aistudenthub.feature.admin.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * Lưu trữ các cấu hình hệ thống dạng key-value.
 * Admin có thể bật/tắt tính năng hoặc điều chỉnh thông số tại runtime mà không cần deploy lại.
 */
@Entity
@Table(name = "system_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemConfig {

    @Id
    @Column(name = "config_key", length = 100)
    private String configKey;

    @Column(name = "config_value", nullable = false, length = 2000)
    private String configValue;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
