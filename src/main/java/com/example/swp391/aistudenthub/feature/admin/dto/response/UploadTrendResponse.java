package com.example.swp391.aistudenthub.feature.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Số lượng documents được upload mỗi ngày.
 * Dùng để vẽ biểu đồ đường (line chart) theo thời gian.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadTrendResponse {

    /** Ngày upload, định dạng "yyyy-MM-dd". */
    private String date;

    /** Số documents được upload trong ngày đó. */
    private long count;
}
