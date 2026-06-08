package com.example.swp391.aistudenthub.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // Auth
    EMAIL_ALREADY_EXISTS("Email đã được sử dụng", HttpStatus.CONFLICT),
    INVALID_CREDENTIALS("Email hoặc mật khẩu không đúng", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN("Token không hợp lệ hoặc đã hết hạn", HttpStatus.UNAUTHORIZED),
    TOKEN_REVOKED("Token đã bị thu hồi", HttpStatus.UNAUTHORIZED),
    USER_NOT_FOUND("Người dùng không tồn tại", HttpStatus.NOT_FOUND),
    ACCOUNT_DISABLED("Tài khoản đã bị vô hiệu hóa", HttpStatus.FORBIDDEN),

    // General
    VALIDATION_ERROR("Dữ liệu đầu vào không hợp lệ", HttpStatus.BAD_REQUEST),
    INTERNAL_ERROR("Lỗi hệ thống, vui lòng thử lại sau", HttpStatus.INTERNAL_SERVER_ERROR),

    // Document
    FILE_TOO_LARGE("File vượt quá dung lượng cho phép (10MB)", HttpStatus.BAD_REQUEST),
    INVALID_FILE_TYPE("Loại file không được hỗ trợ", HttpStatus.BAD_REQUEST),
    EMPTY_FILE("File không được rỗng", HttpStatus.BAD_REQUEST),
    DOCUMENT_NOT_FOUND("Tài liệu không tồn tại", HttpStatus.NOT_FOUND),
    UPLOAD_FAILED("Upload file thất bại, vui lòng thử lại", HttpStatus.INTERNAL_SERVER_ERROR),
    FORBIDDEN_ACCESS("Bạn không có quyền thực hiện thao tác này", HttpStatus.FORBIDDEN);

    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(String message, HttpStatus httpStatus) {
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
