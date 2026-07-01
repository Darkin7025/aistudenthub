package com.example.swp391.aistudenthub.feature.admin.controller;

import com.example.swp391.aistudenthub.common.dto.ApiResponse;
import com.example.swp391.aistudenthub.feature.admin.dto.response.AdminDashboardStatsResponse;
import com.example.swp391.aistudenthub.feature.admin.dto.response.AiUsageResponse;
import com.example.swp391.aistudenthub.feature.admin.dto.response.DocumentTypeStatResponse;
import com.example.swp391.aistudenthub.feature.admin.dto.response.UploadTrendResponse;
import com.example.swp391.aistudenthub.feature.admin.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin API — Dashboard thống kê.
 * Cung cấp số liệu tổng quan, biểu đồ loại file, xu hướng upload và tỷ lệ AI usage.
 */
@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Dashboard", description = "Thống kê tổng quan hệ thống cho Admin dashboard")
public class AdminDashboardController {

    private final AdminService adminService;

    /**
     * GET /api/v1/admin/dashboard/stats
     * Tổng số users, documents, chat sessions và số users bị vô hiệu hóa.
     */
    @GetMapping("/stats")
    @Operation(
        summary = "Tổng quan thống kê hệ thống",
        description = "Trả về tổng số users đang hoạt động, tài liệu, chat sessions và users bị vô hiệu hóa."
    )
    public ResponseEntity<ApiResponse<AdminDashboardStatsResponse>> getDashboardStats() {
        AdminDashboardStatsResponse stats = adminService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * GET /api/v1/admin/dashboard/document-types
     * Số tài liệu phân theo loại file (PDF, Word, Excel, Image...).
     * Dùng để vẽ biểu đồ tròn (pie chart) hoặc cột (bar chart).
     */
    @GetMapping("/document-types")
    @Operation(
        summary = "Thống kê tài liệu theo loại file",
        description = "Trả về danh sách loại file kèm số lượng. Mỗi entry có fileType (MIME), label (thân thiện) và count."
    )
    public ResponseEntity<ApiResponse<List<DocumentTypeStatResponse>>> getDocumentTypeStats() {
        List<DocumentTypeStatResponse> stats = adminService.getDocumentTypeStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * GET /api/v1/admin/dashboard/upload-trend?days=30
     * Số documents được upload mỗi ngày trong N ngày gần nhất.
     * Mặc định 30 ngày, tối đa 365 ngày.
     * Dùng để vẽ biểu đồ đường (line chart) theo thời gian.
     */
    @GetMapping("/upload-trend")
    @Operation(
        summary = "Xu hướng upload theo ngày",
        description = "Trả về số documents upload mỗi ngày trong khoảng `days` ngày gần nhất (mặc định 30, tối đa 365)."
    )
    public ResponseEntity<ApiResponse<List<UploadTrendResponse>>> getUploadTrend(
            @RequestParam(defaultValue = "30") int days) {
        List<UploadTrendResponse> trend = adminService.getUploadTrend(days);
        return ResponseEntity.ok(ApiResponse.success(trend));
    }

    /**
     * GET /api/v1/admin/dashboard/ai-usage
     * Tỷ lệ documents đã được chat với AI so với tổng số documents.
     */
    @GetMapping("/ai-usage")
    @Operation(
        summary = "Tỷ lệ sử dụng AI chat",
        description = "Trả về tổng số tài liệu, số tài liệu đã được dùng với AI chat và tỷ lệ phần trăm."
    )
    public ResponseEntity<ApiResponse<AiUsageResponse>> getAiUsage() {
        AiUsageResponse usage = adminService.getAiUsage();
        return ResponseEntity.ok(ApiResponse.success(usage));
    }
}
