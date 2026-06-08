package com.example.swp391.aistudenthub.feature.document.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FolderResponse {
    private UUID id;
    private String name;
    private String description;
    private String color;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private Long documentCount;
    private UUID parentId;
}
