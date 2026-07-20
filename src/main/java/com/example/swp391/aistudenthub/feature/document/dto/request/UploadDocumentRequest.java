package com.example.swp391.aistudenthub.feature.document.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Thông tin tài liệu cần upload")
public class UploadDocumentRequest {

    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 255, message = "Tiêu đề tối đa 255 ký tự")
    @Schema(description = "Tiêu đề tài liệu")
    private String title;

    @Size(max = 1000, message = "Mô tả tối đa 1000 ký tự")
    @Schema(description = "Mô tả ngắn về tài liệu")
    private String description;

    @Size(max = 255, message = "Môn học tối đa 255 ký tự")
    @Schema(description = "Tên môn học")
    private String subject;

    @Size(max = 255, message = "Chuyên ngành tối đa 255 ký tự")
    @Schema(description = "Chuyên ngành", example = "Công nghệ thông tin")
    private String major;

    @Size(max = 100, message = "Loại tài liệu tối đa 100 ký tự")
    @Schema(description = "Loại tài liệu", example = "PDF")
    private String documentType;

    @Schema(description = "Quyền hiển thị tài liệu", example = "PUBLIC")
    private com.example.swp391.aistudenthub.feature.document.enums.DocumentVisibility visibility;

    @Schema(description = "ID thư mục chứa tài liệu (để trống nếu không có)", example = "null", nullable = true)
    private UUID folderId;

    @Schema(description = "Metadata tuỳ chỉnh dưới dạng JSON array (để trống nếu không cần)", example = "null", nullable = true)
    private String customMetadata;
}
