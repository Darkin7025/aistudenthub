package com.example.swp391.aistudenthub.feature.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Số lượng documents theo từng loại file (fileType).
 * Ví dụ: application/pdf -> 42, image/jpeg -> 15.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentTypeStatResponse {

    /** MIME type hoặc loại file, ví dụ: "application/pdf". */
    private String fileType;

    /** Nhãn hiển thị thân thiện, ví dụ: "PDF". */
    private String label;

    /** Số lượng documents thuộc loại này. */
    private long count;
}
