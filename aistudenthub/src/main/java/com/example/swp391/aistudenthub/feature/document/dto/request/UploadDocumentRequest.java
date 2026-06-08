package com.example.swp391.aistudenthub.feature.document.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class UploadDocumentRequest {

    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 255, message = "Tiêu đề tối đa 255 ký tự")
    private String title;

    @Size(max = 1000, message = "Mô tả tối đa 1000 ký tự")
    private String description;

    @Size(max = 255, message = "Môn học tối đa 255 ký tự")
    private String subject;

    @Size(max = 255, message = "Chuyên ngành tối đa 255 ký tự")
    private String major;

    @Size(max = 100, message = "Loại tài liệu tối đa 100 ký tự")
    private String documentType;

    private com.example.swp391.aistudenthub.feature.document.enums.DocumentVisibility visibility;

    private UUID folderId;

    private String customMetadata;
}
