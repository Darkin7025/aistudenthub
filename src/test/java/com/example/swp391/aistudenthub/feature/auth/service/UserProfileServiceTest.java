package com.example.swp391.aistudenthub.feature.auth.service;

import com.example.swp391.aistudenthub.exception.AppException;
import com.example.swp391.aistudenthub.exception.ErrorCode;
import com.example.swp391.aistudenthub.feature.auth.dto.ChangePasswordRequest;
import com.example.swp391.aistudenthub.feature.auth.dto.UpdateProfileRequest;
import com.example.swp391.aistudenthub.feature.auth.dto.UserProfileResponse;
import com.example.swp391.aistudenthub.feature.auth.entity.Role;
import com.example.swp391.aistudenthub.feature.auth.entity.User;
import com.example.swp391.aistudenthub.feature.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserProfileService userProfileService;

    @Test
    void updateProfile_shouldNormalizeEmailAndResetVerificationWhenEmailChanges() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email("old@example.com")
                .fullName("Old Name")
                .password("encoded-password")
                .role(Role.USER)
                .active(true)
                .emailVerified(true)
                .build();

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("New Name");
        request.setEmail("  NEW@Example.com  ");
        request.setAvatarUrl("  https://cdn.example.com/avatar.png  ");

        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailAndDeletedAtIsNull("new@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileResponse response = userProfileService.updateProfile(userId, request);

        assertEquals("new@example.com", response.getEmail());
        assertEquals("New Name", response.getFullName());
        assertEquals("https://cdn.example.com/avatar.png", response.getAvatarUrl());
        assertFalse(response.isEmailVerified());
        verify(userRepository).save(user);
    }

    @Test
    void updateProfile_shouldThrowWhenEmailAlreadyExistsForAnotherUser() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email("old@example.com")
                .fullName("Old Name")
                .password("encoded-password")
                .role(Role.USER)
                .active(true)
                .build();

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("New Name");
        request.setEmail("taken@example.com");

        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailAndDeletedAtIsNull("taken@example.com")).thenReturn(true);

        AppException exception = assertThrows(AppException.class,
                () -> userProfileService.updateProfile(userId, request));

        assertEquals(ErrorCode.EMAIL_ALREADY_EXISTS, exception.getErrorCode());
    }

    @Test
    void changePassword_shouldThrowWhenConfirmationDoesNotMatch() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email("user@example.com")
                .fullName("User Name")
                .password("encoded-password")
                .role(Role.USER)
                .active(true)
                .build();

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("Current@123");
        request.setNewPassword("NewPassword@123");
        request.setConfirmPassword("Mismatch@123");

        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));

        AppException exception = assertThrows(AppException.class,
                () -> userProfileService.changePassword(userId, request));

        assertEquals(ErrorCode.PASSWORD_CONFIRMATION_MISMATCH, exception.getErrorCode());
        verify(passwordEncoder, never()).matches(any(), any());
    }
}
