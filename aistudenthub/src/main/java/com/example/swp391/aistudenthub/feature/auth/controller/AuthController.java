package com.example.swp391.aistudenthub.feature.auth.controller;

import com.example.swp391.aistudenthub.common.dto.ApiResponse;
import com.example.swp391.aistudenthub.common.dto.MessageResponse;
import com.example.swp391.aistudenthub.feature.auth.dto.AuthResponse;
import com.example.swp391.aistudenthub.feature.auth.dto.ForgotPasswordRequest;
import com.example.swp391.aistudenthub.feature.auth.dto.LoginRequest;
import com.example.swp391.aistudenthub.feature.auth.dto.RegisterRequest;
import com.example.swp391.aistudenthub.feature.auth.dto.ResetPasswordRequest;
import com.example.swp391.aistudenthub.feature.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * ĐĂNG KÝ
     * POST /api/v1/auth/register
     * Body: { "fullName": "...", "email": "...", "password": "..." }
     * → hash password → lưu DB
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<MessageResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(authService.register(request)));
    }

    /**
     * ĐĂNG NHẬP
     * POST /api/v1/auth/login
     * Body: { "email": "...", "password": "..." }
     * → kiểm tra password → generate JWT → trả token về FE
     * Response: { "token": "eyJ...", "tokenType": "Bearer", "expiresIn": 900 }
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.login(request)));
    }

    /**
     * ĐĂNG XUẤT
     * POST /api/v1/auth/logout
     * Header: Authorization: Bearer <token>
     * → Server trả 200, FE tự xóa token khỏi storage
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<MessageResponse>> logout() {
        return ResponseEntity.ok(ApiResponse.success(authService.logout()));
    }

    /**
     * QUÊN MẬT KHẨU — gửi email reset link
     * POST /api/v1/auth/forgot-password
     * Body: { "email": "..." }
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<MessageResponse>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.forgotPassword(request)));
    }

    /**
     * ĐẶT LẠI MẬT KHẨU
     * POST /api/v1/auth/reset-password
     * Body: { "token": "...", "newPassword": "..." }
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<MessageResponse>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.resetPassword(request)));
    }
}
