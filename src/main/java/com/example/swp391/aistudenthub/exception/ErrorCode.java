package com.example.swp391.aistudenthub.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // Auth
    EMAIL_ALREADY_EXISTS("Email \u0111\u00e3 \u0111\u01b0\u1ee3c s\u1eed d\u1ee5ng", HttpStatus.CONFLICT),
    INVALID_CREDENTIALS("Email ho\u1eb7c m\u1eadt kh\u1ea9u kh\u00f4ng \u0111\u00fang", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN("Token kh\u00f4ng h\u1ee3p l\u1ec7 ho\u1eb7c \u0111\u00e3 h\u1ebft h\u1ea1n", HttpStatus.UNAUTHORIZED),
    TOKEN_REVOKED("Token \u0111\u00e3 b\u1ecb thu h\u1ed3i", HttpStatus.UNAUTHORIZED),
    USER_NOT_FOUND("Ng\u01b0\u1eddi d\u00f9ng kh\u00f4ng t\u1ed3n t\u1ea1i", HttpStatus.NOT_FOUND),
    ACCOUNT_DISABLED("T\u00e0i kho\u1ea3n \u0111\u00e3 b\u1ecb v\u00f4 hi\u1ec7u h\u00f3a", HttpStatus.FORBIDDEN),
    CURRENT_PASSWORD_INCORRECT("M\u1eadt kh\u1ea9u hi\u1ec7n t\u1ea1i kh\u00f4ng \u0111\u00fang", HttpStatus.BAD_REQUEST),
    PASSWORD_CONFIRMATION_MISMATCH("X\u00e1c nh\u1eadn m\u1eadt kh\u1ea9u kh\u00f4ng kh\u1edbp", HttpStatus.BAD_REQUEST),
    NEW_PASSWORD_MUST_BE_DIFFERENT("M\u1eadt kh\u1ea9u m\u1edbi ph\u1ea3i kh\u00e1c m\u1eadt kh\u1ea9u hi\u1ec7n t\u1ea1i", HttpStatus.BAD_REQUEST),

    // General
    VALIDATION_ERROR("D\u1eef li\u1ec7u \u0111\u1ea7u v\u00e0o kh\u00f4ng h\u1ee3p l\u1ec7", HttpStatus.BAD_REQUEST),
    INTERNAL_ERROR("L\u1ed7i h\u1ec7 th\u1ed1ng, vui l\u00f2ng th\u1eed l\u1ea1i sau", HttpStatus.INTERNAL_SERVER_ERROR),

    // Document
    FILE_TOO_LARGE("File v\u01b0\u1ee3t qu\u00e1 dung l\u01b0\u1ee3ng cho ph\u00e9p (10MB)", HttpStatus.BAD_REQUEST),
    INVALID_FILE_TYPE("Lo\u1ea1i file kh\u00f4ng \u0111\u01b0\u1ee3c h\u1ed7 tr\u1ee3", HttpStatus.BAD_REQUEST),
    EMPTY_FILE("File kh\u00f4ng \u0111\u01b0\u1ee3c r\u1ed7ng", HttpStatus.BAD_REQUEST),
    DOCUMENT_NOT_FOUND("T\u00e0i li\u1ec7u kh\u00f4ng t\u1ed3n t\u1ea1i", HttpStatus.NOT_FOUND),
    UPLOAD_FAILED("Upload file th\u1ea5t b\u1ea1i, vui l\u00f2ng th\u1eed l\u1ea1i", HttpStatus.INTERNAL_SERVER_ERROR),
    DOCUMENT_CONTENT_NOT_AVAILABLE("N\u1ed9i dung t\u00e0i li\u1ec7u ch\u01b0a \u0111\u01b0\u1ee3c x\u1eed l\u00fd ho\u1eb7c lo\u1ea1i file n\u00e0y ch\u01b0a h\u1ed7 tr\u1ee3 AI.", HttpStatus.BAD_REQUEST),
    FORBIDDEN_ACCESS("B\u1ea1n kh\u00f4ng c\u00f3 quy\u1ec1n th\u1ef1c hi\u1ec7n thao t\u00e1c n\u00e0y", HttpStatus.FORBIDDEN),

    // Chat
    CHAT_SESSION_NOT_FOUND("Cu\u1ed9c h\u1ed9i tho\u1ea1i kh\u00f4ng t\u1ed3n t\u1ea1i", HttpStatus.NOT_FOUND),
    CHAT_SESSION_DOCUMENT_MISMATCH("Cu\u1ed9c h\u1ed9i tho\u1ea1i \u0111ang g\u1eafn v\u1edbi t\u00e0i li\u1ec7u kh\u00e1c", HttpStatus.BAD_REQUEST),
    AI_SERVICE_UNAVAILABLE("D\u1ecbch v\u1ee5 AI t\u1ea1m th\u1eddi kh\u00f4ng kh\u1ea3 d\u1ee5ng, vui l\u00f2ng th\u1eed l\u1ea1i sau", HttpStatus.SERVICE_UNAVAILABLE),

    // Folder
    FOLDER_NOT_FOUND("Th\u01b0 m\u1ee5c kh\u00f4ng t\u1ed3n t\u1ea1i", HttpStatus.NOT_FOUND),
    FOLDER_NOT_EMPTY("Th\u01b0 m\u1ee5c kh\u00f4ng r\u1ed7ng (\u0111ang ch\u1ee9a t\u00e0i li\u1ec7u ho\u1eb7c th\u01b0 m\u1ee5c con)", HttpStatus.BAD_REQUEST);

    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(String message, HttpStatus httpStatus) {
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
