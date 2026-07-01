package com.example.swp391.aistudenthub.feature.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Tổng quan thống kê hệ thống cho Admin dashboard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardStatsResponse {

    /** Tổng số user đang hoạt động (chưa bị xóa mềm). */
    private long totalUsers;

    /** Tổng số tài liệu chưa xóa mềm. */
    private long totalDocuments;

    /** Tổng số chat sessions. */
    private long totalChatSessions;

    /** Số user đang bị vô hiệu hóa (active=false). */
    private long disabledUsers;
}
