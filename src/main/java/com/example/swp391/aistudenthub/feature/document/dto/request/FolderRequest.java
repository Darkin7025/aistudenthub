package com.example.swp391.aistudenthub.feature.document.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FolderRequest {

    @NotBlank(message = "Tên thư mục không được để trống")
    private String name;

    private String description;
    private String color;
    private java.util.UUID parentId;
}
