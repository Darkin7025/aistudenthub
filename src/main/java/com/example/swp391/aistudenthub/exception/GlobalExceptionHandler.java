package com.example.swp391.aistudenthub.exception;

import com.example.swp391.aistudenthub.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import com.example.swp391.aistudenthub.feature.admin.entity.LogLevel;
import com.example.swp391.aistudenthub.feature.admin.service.SystemLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.example.swp391.aistudenthub.feature.auth.entity.User;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final SystemLogService systemLogService;

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Void>> handleAppException(AppException ex) {
        return ResponseEntity
                .status(Objects.requireNonNull(ex.getErrorCode().getHttpStatus()))
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        // Gom tất cả lỗi field thành một chuỗi dễ đọc
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(detail));
    }

    @ExceptionHandler({ConstraintViolationException.class, MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class})
    public ResponseEntity<ApiResponse<Void>> handleInvalidRequest(Exception ex) {
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(ErrorCode.VALIDATION_ERROR.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(Exception ex, HttpServletRequest request) {
        log.error("[500] Unhandled exception: {}", ex.getMessage(), ex);

        try {
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User actor = authentication != null && authentication.getPrincipal() instanceof User user ? user : null;
            systemLogService.logError(ex.getMessage(), sw.toString(), "GlobalExceptionHandler",
                    request.getMethod(), request.getRequestURI(), 500, request.getRemoteAddr(),
                    actor != null ? actor.getId() : null, actor != null ? actor.getEmail() : null);
        } catch (Exception loggingEx) {
            log.error("Failed to write to SystemLogService", loggingEx);
        }

        return ResponseEntity
                .internalServerError()
                .body(ApiResponse.error(ErrorCode.INTERNAL_ERROR.getMessage()));
    }
}
