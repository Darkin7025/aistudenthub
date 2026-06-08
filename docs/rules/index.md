# Rules Index — aistudenthub (Backend)

> Bản đồ chỉ dẫn AI đọc rule nào theo loại task. **Luôn đọc `AGENTS.md` trước**, sau đó mở file rule tương ứng.

## Cách dùng

```
Task → Đọc rule files được đánh dấu ★ (bắt buộc) + rule bổ sung (khuyến nghị)
```

---

## Task routing — SWP391

### [Document] Tải xuống tài liệu

API/Controller phục vụ download file từ Cloudinary hoặc proxy stream.

| Ưu tiên | Rule file | Nội dung |
|---|---|---|
| ★ | [`document.md`](./document.md) | Entity, service, ownership, endpoint pattern |
| ★ | [`cloud-storage.md`](./cloud-storage.md) | Cloudinary URL, signed download, content-type |
| ★ | [`api.md`](./api.md) | Response shape, HTTP headers cho file download |
| | [`auth.md`](./auth.md) | JWT required, ownership check |

**Endpoint gợi ý:** `GET /api/v1/documents/{id}/download`

---

### [Document] Chỉnh sửa thông tin tài liệu

Cập nhật title, description, subject (môn học).

| Ưu tiên | Rule file | Nội dung |
|---|---|---|
| ★ | [`document.md`](./document.md) | Update flow, validation, DTO |
| ★ | [`business.md`](./business.md) | Business rules, field subject |
| ★ | [`database.md`](./database.md) | Migration entity, index |
| | [`api.md`](./api.md) | PATCH/PUT convention |
| | [`auth.md`](./auth.md) | Chỉ owner được sửa |

**Endpoint gợi ý:** `PATCH /api/v1/documents/{id}`

**Lưu ý:** Entity `Document` chưa có field `subject` — cần thêm.

---

### [Document] Tìm kiếm tài liệu

Search theo title, description (full-text hoặc LIKE).

| Ưu tiên | Rule file | Nội dung |
|---|---|---|
| ★ | [`document.md`](./document.md) | Repository query, pagination |
| ★ | [`database.md`](./database.md) | Index, query performance |
| ★ | [`api.md`](./api.md) | Query params `q`, `page`, `size` |
| | [`business.md`](./business.md) | Scope search (my docs vs public) |

**Endpoint gợi ý:** `GET /api/v1/documents/search?q=...`

---

### [Document] Lọc tài liệu theo môn học

Filter theo subject/chuyên ngành.

| Ưu tiên | Rule file | Nội dung |
|---|---|---|
| ★ | [`document.md`](./document.md) | Filter query, combine với search |
| ★ | [`business.md`](./business.md) | Danh sách môn học, enum vs free text |
| ★ | [`database.md`](./database.md) | Column subject, index |
| | [`api.md`](./api.md) | Query param `subject` |

**Endpoint gợi ý:** `GET /api/v1/documents?subject=SWP391` hoặc gộp vào `/search`

---

### [Cloud Storage] Xem trạng thái upload

Theo dõi tiến trình/trạng thái file đang upload.

| Ưu tiên | Rule file | Nội dung |
|---|---|---|
| ★ | [`cloud-storage.md`](./cloud-storage.md) | Upload job, status enum, async pattern |
| ★ | [`document.md`](./document.md) | Liên kết upload → document record |
| | [`api.md`](./api.md) | Polling endpoint hoặc SSE |
| | [`database.md`](./database.md) | Bảng upload_jobs nếu cần |

**Endpoint gợi ý:** `GET /api/v1/documents/uploads/{jobId}/status`

---

### [Cloud Storage] Preview file

Xem trước PDF/ảnh trực tiếp.

| Ưu tiên | Rule file | Nội dung |
|---|---|---|
| ★ | [`cloud-storage.md`](./cloud-storage.md) | Cloudinary transformation URL, preview |
| ★ | [`document.md`](./document.md) | File type check, access control |
| | [`api.md`](./api.md) | Trả preview URL hoặc proxy |
| | [`auth.md`](./auth.md) | Private doc → chỉ owner |

**Endpoint gợi ý:** `GET /api/v1/documents/{id}/preview-url`

---

### [AI Chatbot] Chat với chatbot

Tích hợp API AI để trò chuyện.

| Ưu tiên | Rule file | Nội dung |
|---|---|---|
| ★ | [`chatbot.md`](./chatbot.md) | Module structure, LLM integration |
| ★ | [`api.md`](./api.md) | Request/response, conversation ID |
| | [`auth.md`](./auth.md) | User-scoped conversations |
| | [`database.md`](./database.md) | Entity Conversation, Message |

**Endpoint gợi ý:** `POST /api/v1/chat/conversations/{id}/messages`

---

### [AI Chatbot] Hỏi đáp về tài liệu (RAG)

Truy vấn nội dung từ file tài liệu.

| Ưu tiên | Rule file | Nội dung |
|---|---|---|
| ★ | [`chatbot.md`](./chatbot.md) | RAG pipeline, embedding, retrieval |
| ★ | [`document.md`](./document.md) | Document context, file parsing |
| ★ | [`cloud-storage.md`](./cloud-storage.md) | Đọc file từ Cloudinary |
| | [`business.md`](./business.md) | Scope: hỏi trong 1 doc vs nhiều doc |

**Endpoint gợi ý:** `POST /api/v1/chat/documents/{documentId}/ask`

---

### [AI Chatbot] Nhận câu trả lời AI (Streaming)

Xử lý SSE/stream response từ model.

| Ưu tiên | Rule file | Nội dung |
|---|---|---|
| ★ | [`chatbot.md`](./chatbot.md) | SSE, WebFlux, chunk protocol |
| ★ | [`api.md`](./api.md) | `text/event-stream`, error mid-stream |
| | [`auth.md`](./auth.md) | Auth trên SSE connection |

**Endpoint gợi ý:** `POST /api/v1/chat/conversations/{id}/messages/stream`

---

### [AI Chatbot] Xem lịch sử chat

Lưu và truy vấn lịch sử hội thoại.

| Ưu tiên | Rule file | Nội dung |
|---|---|---|
| ★ | [`chatbot.md`](./chatbot.md) | Entity, pagination history |
| ★ | [`database.md`](./database.md) | Tables conversations, messages |
| | [`api.md`](./api.md) | List endpoint, cursor pagination |
| | [`auth.md`](./auth.md) | User chỉ xem chat của mình |

**Endpoint gợi ý:** `GET /api/v1/chat/conversations`, `GET /api/v1/chat/conversations/{id}/messages`

---

## Rule files — mục lục đầy đủ

| File | Phạm vi |
|---|---|
| [`api.md`](./api.md) | REST conventions, ApiResponse, pagination, streaming |
| [`auth.md`](./auth.md) | JWT, SecurityConfig, ownership, roles |
| [`backend.md`](./backend.md) | Java/Spring coding standards, package structure |
| [`business.md`](./business.md) | Domain rules, subject taxonomy, visibility |
| [`chatbot.md`](./chatbot.md) | AI integration, RAG, SSE, conversation |
| [`cloud-storage.md`](./cloud-storage.md) | Cloudinary upload/download/preview |
| [`database.md`](./database.md) | SQL Server, JPA, migration, indexing |
| [`document.md`](./document.md) | Document CRUD, search, filter, download |
| [`testing.md`](./testing.md) | Unit test, integration test, .http files |

---

## Task chung (không thuộc feature cụ thể)

| Task | Đọc |
|---|---|
| Thêm dependency Maven | `backend.md` |
| Sửa SecurityConfig | `auth.md`, `api.md` |
| Thêm ErrorCode mới | `api.md`, `backend.md` |
| Viết test | `testing.md` + rule feature liên quan |
| Thêm entity mới | `database.md`, `backend.md` |
