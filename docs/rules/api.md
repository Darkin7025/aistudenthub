# API Conventions

> Quy chuẩn REST API cho **aistudenthub**.

## Base URL

```
http://localhost:8080/api/v1
```

Versioning qua path prefix `/api/v1/`. Không đổi version khi thêm endpoint mới trong cùng contract.

## Response wrapper

Mọi JSON response dùng `ApiResponse<T>`:

```json
{
  "success": true,
  "data": { },
  "message": "Tùy chọn",
  "timestamp": "2026-06-05T10:00:00+07:00"
}
```

```java
// Success
return ResponseEntity.ok(ApiResponse.success(data));
return ResponseEntity.status(HttpStatus.CREATED)
    .body(ApiResponse.success(data, "Tạo thành công"));

// Error — qua GlobalExceptionHandler, không viết tay trong controller
throw new AppException(ErrorCode.DOCUMENT_NOT_FOUND);
```

## HTTP methods & status

| Action | Method | Status |
|---|---|---|
| Create | `POST` | `201 Created` |
| Read | `GET` | `200 OK` |
| Partial update | `PATCH` | `200 OK` |
| Full replace | `PUT` | `200 OK` |
| Delete (soft) | `DELETE` | `200 OK` |
| Not found | — | `404` |
| Forbidden | — | `403` |
| Validation | — | `400` |
| Unauthorized | — | `401` |

## Authentication

```
Authorization: Bearer <jwt_access_token>
```

Lấy user: `@AuthenticationPrincipal User currentUser`

Public endpoints (không cần token):
- `/api/v1/auth/**`
- `/swagger-ui/**`, `/v3/api-docs/**`

## Pagination (chuẩn đề xuất)

Query params:
```
?page=0&size=20&sort=createdAt,desc
```

Response data shape:
```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 100,
  "totalPages": 5
}
```

Tạo `PageResponse<T>` trong `common/dto/` khi implement.

## Search & filter query params

```
GET /api/v1/documents/search?q=java&subject=PRJ301&page=0&size=10
```

| Param | Mô tả |
|---|---|
| `q` | Keyword tìm trong title + description |
| `subject` | Lọc theo môn học |
| `page`, `size` | Phân trang |
| `sort` | Sắp xếp |

## File upload

```
POST /api/v1/documents/upload
Content-Type: multipart/form-data

file: <binary>
title: string (required)
description: string (optional)
subject: string (optional — khi đã thêm field)
```

Giới hạn: 10 MB, MIME types trong `DocumentService.ALLOWED_MIME_TYPES`.

## File download

```
GET /api/v1/documents/{id}/download
Authorization: Bearer <token>
```

Option A — Redirect:
```java
return ResponseEntity.status(HttpStatus.FOUND)
    .location(URI.create(signedUrl))
    .build();
```

Option B — Proxy stream:
```java
return ResponseEntity.ok()
    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
    .contentType(MediaType.parseMediaType(fileType))
    .body(resource);
```

## SSE Streaming (Chatbot)

```
POST /api/v1/chat/conversations/{id}/messages/stream
Accept: text/event-stream
Authorization: Bearer <token>

Body: { "content": "Giải thích chương 3" }
```

Response:
```
event: message
data: {"delta":"Xin"}

event: message
data: {"delta":" chào"}

event: done
data: {"messageId":"uuid"}
```

Implement bằng `SseEmitter` (MVC) hoặc WebFlux `Flux<ServerSentEvent>`.

## Swagger

- URL: `http://localhost:8080/swagger-ui.html`
- Thêm `@Tag`, `@Operation`, `@ApiResponse` cho endpoint mới
- Config tại `config/OpenApiConfig.java`

## Endpoint registry

### Auth (public)
| Method | Path |
|---|---|
| POST | `/api/v1/auth/register` |
| POST | `/api/v1/auth/login` |
| POST | `/api/v1/auth/logout` |
| POST | `/api/v1/auth/forgot-password` |
| POST | `/api/v1/auth/reset-password` |

### Document (protected)
| Method | Path | Status |
|---|---|---|
| POST | `/api/v1/documents/upload` | ✅ Có |
| GET | `/api/v1/documents/my` | ✅ Có |
| GET | `/api/v1/documents/{id}` | ✅ Có |
| DELETE | `/api/v1/documents/{id}` | ✅ Có |
| GET | `/api/v1/documents/{id}/download` | ❌ Cần làm |
| PATCH | `/api/v1/documents/{id}` | ❌ Cần làm |
| GET | `/api/v1/documents/search` | ❌ Cần làm |
| GET | `/api/v1/documents/{id}/preview-url` | ❌ Cần làm |

### Chat (protected) — chưa có
| Method | Path |
|---|---|
| GET | `/api/v1/chat/conversations` |
| POST | `/api/v1/chat/conversations` |
| GET | `/api/v1/chat/conversations/{id}/messages` |
| POST | `/api/v1/chat/conversations/{id}/messages` |
| POST | `/api/v1/chat/conversations/{id}/messages/stream` |
| POST | `/api/v1/chat/documents/{documentId}/ask` |
