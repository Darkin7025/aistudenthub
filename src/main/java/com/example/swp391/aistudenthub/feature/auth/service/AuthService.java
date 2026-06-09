package com.example.swp391.aistudenthub.feature.auth.service;

import com.example.swp391.aistudenthub.exception.AppException;
import com.example.swp391.aistudenthub.exception.ErrorCode;
import com.example.swp391.aistudenthub.common.dto.MessageResponse;
import com.example.swp391.aistudenthub.feature.auth.dto.AuthResponse;
import com.example.swp391.aistudenthub.feature.auth.dto.ForgotPasswordRequest;
import com.example.swp391.aistudenthub.feature.auth.dto.LoginRequest;
import com.example.swp391.aistudenthub.feature.auth.dto.RefreshTokenRequest;
import com.example.swp391.aistudenthub.feature.auth.dto.RegisterRequest;
import com.example.swp391.aistudenthub.feature.auth.dto.ResetPasswordRequest;
import com.example.swp391.aistudenthub.feature.auth.entity.PasswordResetToken;
import com.example.swp391.aistudenthub.feature.auth.entity.RefreshToken;
import com.example.swp391.aistudenthub.feature.auth.entity.User;
import com.example.swp391.aistudenthub.feature.auth.repository.PasswordResetTokenRepository;
import com.example.swp391.aistudenthub.feature.auth.repository.RefreshTokenRepository;
import com.example.swp391.aistudenthub.feature.auth.repository.UserRepository;
import com.example.swp391.aistudenthub.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration; // ms

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    // ----------------------------------------------------------------
    // 1. ĐĂNG KÝ → lưu password plain text → lưu DB
    // ----------------------------------------------------------------
    @Transactional
    public MessageResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailAndDeletedAtIsNull(request.getEmail().toLowerCase())) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        userRepository.save(user);
        return new MessageResponse("Đăng ký thành công! Vui lòng đăng nhập.");
    }

    // ----------------------------------------------------------------
    // 2. ĐĂNG NHẬP → kiểm tra password → generate JWT → trả token về FE
    // ----------------------------------------------------------------
    public AuthResponse login(LoginRequest request) {
        // Kiểm tra credentials
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail().toLowerCase(),
                            request.getPassword()
                    )
            );
        } catch (BadCredentialsException e) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        // Load user từ DB
        User user = userRepository.findByEmailAndDeletedAtIsNull(request.getEmail().toLowerCase())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (!user.isEnabled()) {
            throw new AppException(ErrorCode.ACCOUNT_DISABLED);
        }

        return buildAuthResponse(user);
    }


    // ----------------------------------------------------------------
    // 2b. LÀM MỚI ACCESS TOKEN (Refresh Token Rotation)
    // ----------------------------------------------------------------
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String hash = sha256(request.getRefreshToken());

        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_TOKEN));

        if (stored.isRevoked()) {
            throw new AppException(ErrorCode.TOKEN_REVOKED);
        }
        if (stored.isExpired()) {
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }

        // Rotate: revoke the current token
        stored.setRevokedAt(OffsetDateTime.now());
        refreshTokenRepository.save(stored);

        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (!user.isEnabled()) {
            throw new AppException(ErrorCode.ACCOUNT_DISABLED);
        }

        return buildAuthResponse(user);
    }


    // ── Private helpers ──────────────────────────────────────────────────────

    /**
     * Generates a new access token + refresh token, persists the refresh token
     * in the DB (hashed), and returns a fully-populated AuthResponse.
     */
    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtUtil.generateAccessToken(user);

        // Generate an opaque refresh token (UUID-based)
        String rawRefreshToken = UUID.randomUUID().toString();
        String hashedRefresh   = sha256(rawRefreshToken);

        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(hashedRefresh)
                .expiresAt(OffsetDateTime.now().plusSeconds(refreshTokenExpiration / 1000))
                .build();
        refreshTokenRepository.save(refreshTokenEntity);

        // Trả token về FE
        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(rawRefreshToken)
                .expiresIn(accessTokenExpiration / 1000)
                .refreshExpiresIn(refreshTokenExpiration / 1000)
                .build();
    }

    // ----------------------------------------------------------------
    // 3. ĐĂNG XUẤT → FE xóa token phía client
    //    Server trả 200 OK (stateless — không cần lưu trạng thái)
    // ----------------------------------------------------------------
    public MessageResponse logout() {
        return new MessageResponse("Đăng xuất thành công.");
    }

    // ----------------------------------------------------------------
    // 4. QUÊN MẬT KHẨU → gửi email chứa link reset
    // ----------------------------------------------------------------
    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        // Luôn trả 200 dù email không tồn tại (tránh tiết lộ email trong hệ thống)
        Optional<User> userOpt = userRepository.findByEmailAndDeletedAtIsNull(
                request.getEmail().toLowerCase());

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            String rawToken = UUID.randomUUID().toString();
            String hashedToken = sha256(rawToken);

            resetTokenRepository.deleteByUserId(user.getId());

            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .userId(user.getId())
                    .tokenHash(hashedToken)
                    .expiresAt(OffsetDateTime.now().plusHours(1))
                    .build();
            resetTokenRepository.save(resetToken);

            emailService.sendPasswordResetEmail(user.getEmail(), rawToken);
        }

        return new MessageResponse("Nếu email tồn tại trong hệ thống, bạn sẽ nhận được hướng dẫn đặt lại mật khẩu.");
    }

    // ----------------------------------------------------------------
    // 5. ĐẶT LẠI MẬT KHẨU → kiểm tra token → cập nhật password mới
    // ----------------------------------------------------------------
    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        String hash = sha256(request.getToken());
        PasswordResetToken resetToken = resetTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_TOKEN));

        if (resetToken.isUsed() || resetToken.isExpired()) {
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }

        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Cập nhật password mới (đã hash)
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Đánh dấu token đã sử dụng (không dùng lại được)
        resetToken.setUsedAt(OffsetDateTime.now());
        resetTokenRepository.save(resetToken);

        return new MessageResponse("Mật khẩu đã được đặt lại thành công. Vui lòng đăng nhập lại.");
    }

    // ----------------------------------------------------------------
    // HELPER — hash token bằng SHA-256 trước khi lưu DB
    // ----------------------------------------------------------------
    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 không khả dụng", e);
        }
    }
}
