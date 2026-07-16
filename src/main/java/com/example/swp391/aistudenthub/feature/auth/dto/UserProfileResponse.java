package com.example.swp391.aistudenthub.feature.auth.dto;

import com.example.swp391.aistudenthub.feature.auth.entity.Role;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class UserProfileResponse {
    private UUID id;
    private String email;
    private String fullName;
    private String avatarUrl;
    private Role role;
    private boolean active;
    private boolean emailVerified;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
