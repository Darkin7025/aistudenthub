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
    DOCUMENT_CONTENT_NOT_AVAILABLE("Nội dung tài liệu chưa được xử lý hoặc loại file này chưa hỗ trợ AI.",
            HttpStatus.BAD_REQUEST),
    FORBIDDEN_ACCESS("Bạn không có quyền thực hiện thao tác này", HttpStatus.FORBIDDEN),

    // Chat
    CHAT_SESSION_NOT_FOUND("Cuộc hội thoại không tồn tại", HttpStatus.NOT_FOUND),
    CHAT_SESSION_DOCUMENT_MISMATCH("Cuộc hội thoại đang gắn với tài liệu khác", HttpStatus.BAD_REQUEST),
    AI_SERVICE_UNAVAILABLE("Dịch vụ AI tạm thời không khả dụng, vui lòng thử lại sau", HttpStatus.SERVICE_UNAVAILABLE),

    // Folder
    FOLDER_NOT_FOUND("Thư mục không tồn tại", HttpStatus.NOT_FOUND),
    FOLDER_NOT_EMPTY("Thư mục không rỗng (đang chứa tài liệu hoặc thư mục con)", HttpStatus.BAD_REQUEST);

    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(String message, HttpStatus httpStatus) {
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
