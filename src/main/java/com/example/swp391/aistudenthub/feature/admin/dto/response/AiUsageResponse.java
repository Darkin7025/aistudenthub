package com.example.swp391.aistudenthub.feature.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Thống kê tỷ lệ AI usage — bao nhiêu tài liệu được chat với AI.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiUsageResponse {

    /** Tổng số documents chưa xóa. */
    private long totalDocuments;

    /** Số documents có ít nhất 1 chat session (đã được dùng với AI). */
    private long documentsWithAiChat;

    /** Số documents chưa được chat với AI. */
    private long documentsWithoutAiChat;

    /**
     * Tỷ lệ phần trăm documents có dùng AI (0–100, làm tròn 2 chữ số).
     * Ví dụ: 42.5
     */
    private double aiUsagePercent;
}
