# Business Rules

> Quy tắc nghiệp vụ domain **AI Student Hub**.

## Domain overview

**AI Student Hub** — nền tảng học tập cho sinh viên:
- Upload và quản lý tài liệu học tập
- Tìm kiếm, lọc theo môn học
- Preview và tải xuống tài liệu
- Chatbot AI hỗ trợ học, hỏi đáp dựa trên tài liệu (RAG)

## User roles

| Role | Quyền |
|---|---|
| `USER` | Upload, quản lý doc của mình, chat, hỏi đáp |
| `ADMIN` | Quản lý toàn hệ thống (chưa enforce) |
| `GUEST` | Chưa dùng |

## Document business rules

### Upload
- Mỗi user upload file cho chính mình (`uploaded_by = userId`)
- Title bắt buộc, trim whitespace
- Description tùy chọn, max 1000 ký tự
- File max 10 MB, chỉ MIME types được phép
- Mặc định `isPublic = true` (có thể đổi khi implement update)

### Ownership
- Chỉ owner mới: update, delete, download (private doc)
- Public doc: mọi user đăng nhập xem/preview được
- Private doc: chỉ owner

### Soft delete
- Xóa = set `deletedAt`, không hiện trong list/search
- Không xóa vật lý file Cloudinary ngay lập tức

### Subject (môn học) — cần thêm

**Đề xuất:** Dùng `varchar(100)` free text hoặc enum.

Danh sách môn gợi ý (SWP391 context):
```
SWP391, PRJ301, DBI202, SWR302, MLN131, ...
```

Option A — Free text: user tự nhập, filter exact match hoặc LIKE
Option B — Enum `Subject`: kiểm soát chặt, cần endpoint list subjects

**Khuyến nghị:** Free text + dropdown gợi ý ở FE.

### Search scope

| Scope | Rule |
|---|---|
| `/documents/my` | Chỉ doc của user, chưa xóa |
| `/documents/search` | Mặc định: doc của user. Mở rộng: thêm public docs của người khác |
| Filter `subject` | Kết hợp với search, AND logic |

### Search logic

```
q match title OR description (case-insensitive, partial)
AND subject = :subject (nếu có)
AND deletedAt IS NULL
AND userId = :currentUser (default scope)
ORDER BY createdAt DESC
```

## Cloud storage rules

- File lưu trên Cloudinary, không local disk
- Folder: `documents/`
- Preview chỉ cho file types hỗ trợ (PDF, image)
- Office files: download only, không preview native

## Chatbot business rules

### Conversation
- Mỗi conversation thuộc 1 user
- Có thể gắn `documentId` (chat trong context 1 tài liệu)
- Title auto-generate từ 50 ký tự đầu message đầu tiên

### Message
- Roles: USER, ASSISTANT, SYSTEM
- Lưu toàn bộ history cho replay
- Max message length: 4000 ký tự (đề xuất)

### RAG
- Chỉ hỏi đáp trên doc user có quyền truy cập
- Doc phải được indexed (parsed + chunked) trước khi ask
- Trả kèm source excerpts khi có thể (transparency)

### AI behavior
- System prompt: trợ lý học tập, trả lời tiếng Việt, dựa trên context tài liệu
- Không bịa nội dung nếu không tìm thấy trong doc → nói "không tìm thấy trong tài liệu"
- Không trả lời câu hỏi ngoài phạm vi học tập (tùy policy team)

## Validation messages

Tất cả message hiển thị user bằng **tiếng Việt**, định nghĩa trong:
- `ErrorCode` enum (backend errors)
- Jakarta `@NotBlank(message=...)` (validation)
- Frontend copy (xem `aistudenthub_FE/docs/rules/i18n.md`)

## Không thuộc scope hiện tại

- Wallet / thanh toán — **không có** trong project
- Multi-tenant / organization
- Comment/review trên tài liệu
- Versioning tài liệu
