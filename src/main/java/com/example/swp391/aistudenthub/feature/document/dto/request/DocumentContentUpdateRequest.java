package com.example.swp391.aistudenthub.feature.document.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentContentUpdateRequest {
    @NotNull(message = "Extracted text content cannot be null")
    private String content;
}
