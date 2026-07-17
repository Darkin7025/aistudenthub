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
    FILE_TOO_LARGE("File vượt quá dung lượng cho phép (10MB)", HttpStatus.BAD_REQUEST),
    INVALID_FILE_TYPE("Loại file không được hỗ trợ", HttpStatus.BAD_REQUEST),
    EMPTY_FILE("File không được rỗng", HttpStatus.BAD_REQUEST),
    DOCUMENT_NOT_FOUND("Tài liệu không tồn tại", HttpStatus.NOT_FOUND),
    UPLOAD_FAILED("Upload file thất bại, vui lòng thử lại", HttpStatus.INTERNAL_SERVER_ERROR),
    DOCUMENT_CONTENT_NOT_AVAILABLE("Nội dung tài liệu chưa được xử lý hoặc loại file này chưa hỗ trợ AI.",
            HttpStatus.BAD_REQUEST),
    FORBIDDEN_ACCESS("Bạn không có quyền thực hiện thao tác này", HttpStatus.FORBIDDEN),

    // Chat
    CHAT_SESSION_NOT_FOUND("Cuộc hội thoại không tồn tại", HttpStatus.NOT_FOUND),
    CHAT_SESSION_DOCUMENT_MISMATCH("Cuộc hội thoại đang gắn với tài liệu khác", HttpStatus.BAD_REQUEST),
    AI_SERVICE_UNAVAILABLE("Dịch vụ AI tạm thời không khả dụng, vui lòng thử lại sau", HttpStatus.SERVICE_UNAVAILABLE),

    // Folder
    FOLDER_NOT_FOUND("Thư mục không tồn tại", HttpStatus.NOT_FOUND),
    FOLDER_NOT_EMPTY("Thư mục không rỗng (đang chứa tài liệu hoặc thư mục con)", HttpStatus.BAD_REQUEST),

    // System
    FEATURE_DISABLED("Tính năng này hiện đang bị tắt", HttpStatus.FORBIDDEN);

    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(String message, HttpStatus httpStatus) {
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
