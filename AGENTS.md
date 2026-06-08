# AGENTS.md — aistudenthub (Backend)

> Luật bắt buộc cho AI Agent khi làm việc trong repository backend **aistudenthub**.

## Vai trò

Spring Boot REST API cho **AI Student Hub**: xác thực JWT, quản lý tài liệu, lưu trữ Cloudinary, AI chatbot (đang phát triển).

**Stack:** Java 17 · Spring Boot 3.5 · Spring Security · JPA · SQL Server · Cloudinary · JWT · SpringDoc OpenAPI

## Quy trình bắt buộc

1. **Đọc** [`docs/rules/index.md`](./docs/rules/index.md) → chọn rule theo task
2. **Đọc** các file rule được chỉ định trước khi viết code
3. **Khám phá** module `feature/{auth,document,chatbot}/` — tái sử dụng pattern hiện có
4. **Chỉ sửa** file liên quan task — không refactor ngoài scope
5. **Không commit** trừ khi user yêu cầu

## Cấu trúc package

```
com.example.swp391.aistudenthub
├── common/dto/          # ApiResponse, MessageResponse
├── config/              # Security, Cloudinary, OpenAPI
├── exception/           # AppException, ErrorCode, GlobalExceptionHandler
├── filter/              # JwtAuthenticationFilter
├── util/                # JwtUtil
└── feature/
    ├── auth/            # controller, service, entity, dto, repository
    ├── document/        # controller, service, entity, dto, repository
    └── chatbot/         # (tạo mới khi implement AI)
```

## Quy tắc kiến trúc

### Layering (bắt buộc)

```
Controller → Service → Repository → Entity
              ↓
         DTO request/response (không expose entity ra API)
```

- **Controller:** nhận request, gọi service, trả `ResponseEntity<ApiResponse<T>>`
- **Service:** business logic, `@Transactional`, throw `AppException(ErrorCode.XXX)`
- **Repository:** Spring Data JPA, query method hoặc `@Query`
- **Entity:** JPA mapping, UUID id, `OffsetDateTime`, soft delete qua `deletedAt`

### API conventions

| Quy tắc | Chi tiết |
|---|---|
| Base path | `/api/v1/{resource}` |
| Response wrapper | `ApiResponse.success(data)` hoặc `.success(data, message)` |
| Auth | `@AuthenticationPrincipal User currentUser` |
| Validation | Jakarta `@Valid` trên DTO, message tiếng Việt |
| Error | `throw new AppException(ErrorCode.XXX)` — không catch rồi nuốt |
| HTTP status | 201 cho create, 200 cho read/update/delete |
| Swagger | Thêm `@Operation`, `@Tag` cho endpoint mới |

### Database

- Engine: **SQL Server**, DB `ai_study_hub`
- ID: `UUID` (`UNIQUEIDENTIFIER`)
- Timestamp: `OffsetDateTime`, auto qua `@PrePersist`/`@PreUpdate`
- Soft delete: set `deletedAt`, query luôn filter `deletedAt IS NULL`
- DDL: `hibernate.ddl-auto=update` — thêm field entity sẽ auto migrate

### Security

- Stateless JWT, header `Authorization: Bearer <token>`
- Public: `/api/v1/auth/**`, Swagger
- Protected: mọi endpoint khác
- Ownership check: so sánh `userId` với `@AuthenticationPrincipal User`

## Module hiện có

### Auth (`feature/auth/`)
- `POST /api/v1/auth/register|login|logout|forgot-password|reset-password`
- JWT 15 phút, password plain text (dev only — ghi chú trong code)

### Document (`feature/document/`)
- `POST /api/v1/documents/upload` — multipart
- `GET /api/v1/documents/my`
- `GET /api/v1/documents/{id}`
- `DELETE /api/v1/documents/{id}`

### Chưa có (task cần implement)
- Download file, update metadata, search, filter by subject
- Upload progress tracking
- Preview URL/signed URL
- Chatbot module (chat, RAG, SSE streaming, history)

## File quan trọng

| File | Mục đích |
|---|---|
| `config/SecurityConfig.java` | Public/protected endpoints |
| `common/dto/ApiResponse.java` | Response wrapper |
| `exception/ErrorCode.java` | Mã lỗi + HTTP status + message VN |
| `feature/document/service/DocumentService.java` | Business logic document |
| `feature/document/service/CloudinaryService.java` | Upload/delete Cloudinary |
| `resources/application.properties` | Config mặc định |

## Cấm

- ❌ Hardcode credentials trong source
- ❌ Trả entity JPA trực tiếp ra API
- ❌ Tạo endpoint không có prefix `/api/v1/`
- ❌ Bỏ qua ownership check trên resource của user
- ❌ Thêm dependency lớn mà không cần thiết
- ❌ Xóa cứng record khi đã có pattern soft delete

## Tài liệu chi tiết

→ [`docs/rules/index.md`](./docs/rules/index.md)
