package com.example.swp391.aistudenthub.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Wrapper chuẩn cho mọi API response.
 *
 * <p>Shape thống nhất:
 * <pre>
 * {
 *   "success"  : true | false,
 *   "data"     : { ... } | null,
 *   "message"  : "..." | null,
 *   "timestamp": "2026-05-23T14:00:00+07:00"
 * }
 * </pre>
 *
 * <p>Sử dụng:
 * <ul>
 *   <li>Success → {@link #success(Object)}</li>
 *   <li>Success + message → {@link #success(Object, String)}</li>
 *   <li>Error  → {@link #error(String)}</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;

    private T data;

    private String message;

    @Builder.Default
    private String timestamp = OffsetDateTime.now().toString();

    // ----------------------------------------------------------------
    // Static factory helpers
    // ----------------------------------------------------------------

    /** Response thành công có data. */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .timestamp(OffsetDateTime.now().toString())
                .build();
    }

    /** Response thành công có data + message tuỳ chọn. */
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .message(message)
                .timestamp(OffsetDateTime.now().toString())
                .build();
    }

    /** Response lỗi, không có data. */
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .timestamp(OffsetDateTime.now().toString())
                .build();
    }
}
