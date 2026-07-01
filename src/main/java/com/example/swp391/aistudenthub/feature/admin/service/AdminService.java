package com.example.swp391.aistudenthub.feature.admin.service;

import com.example.swp391.aistudenthub.feature.admin.dto.request.UpdateSystemConfigRequest;
import com.example.swp391.aistudenthub.feature.admin.dto.request.UpdateUserStatusRequest;
import com.example.swp391.aistudenthub.feature.admin.dto.response.AdminDashboardStatsResponse;
import com.example.swp391.aistudenthub.feature.admin.dto.response.AdminUserResponse;
import com.example.swp391.aistudenthub.feature.admin.dto.response.AiUsageResponse;
import com.example.swp391.aistudenthub.feature.admin.dto.response.DocumentTypeStatResponse;
import com.example.swp391.aistudenthub.feature.admin.dto.response.SystemConfigResponse;
import com.example.swp391.aistudenthub.feature.admin.dto.response.UploadTrendResponse;
import com.example.swp391.aistudenthub.common.dto.MessageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Service interface cho các chức năng Admin.
 */
public interface AdminService {

    // ---- User Management ----

    Page<AdminUserResponse> getAllUsers(String keyword, Pageable pageable);

    AdminUserResponse getUserById(UUID userId);

    AdminUserResponse updateUserStatus(UUID userId, UpdateUserStatusRequest request);

    MessageResponse softDeleteUser(UUID userId);

    // ---- Dashboard ----

    AdminDashboardStatsResponse getDashboardStats();

    List<DocumentTypeStatResponse> getDocumentTypeStats();

    List<UploadTrendResponse> getUploadTrend(int days);

    AiUsageResponse getAiUsage();

    // ---- System Config ----

    List<SystemConfigResponse> getAllConfigs();

    List<SystemConfigResponse> updateConfigs(UpdateSystemConfigRequest request);
}
