# Cloud Storage (Cloudinary)

> Quy tắc lưu trữ file qua **Cloudinary** trong **aistudenthub**.

## Kiến trúc

```
Upload request → DocumentService.validateFile()
              → CloudinaryService.upload(file)
              → Lưu metadata (url, public_id) vào Document entity
```

CloudinaryService **không biết** về Document entity — chỉ upload/delete file.

## Files

| File | Vai trò |
|---|---|
| `config/CloudinaryConfig.java` | Bean `Cloudinary` từ env |
| `feature/document/service/CloudinaryService.java` | upload(), delete() |
| `resources/application.properties` | `cloudinary.*` properties |

## Config

```properties
cloudinary.cloud-name=${CLOUDINARY_CLOUD_NAME}
cloudinary.api-key=${CLOUDINARY_API_KEY}
cloudinary.api-secret=${CLOUDINARY_API_SECRET}
```

Credentials trong `application-local.properties` (gitignored).

## Upload flow hiện tại

```java
Map<String, String> result = cloudinaryService.upload(file);
// result: { "url": secure_url, "public_id": "documents/xxx" }
```

Options:
- `resource_type: auto` — tự detect image/video/raw
- `folder: documents` — tổ chức file

## Metadata lưu DB

| Field | Nguồn |
|---|---|
| `fileUrl` | `secure_url` |
| `storagePublicId` | `public_id` |
| `storageKey` | `public_id` |
| `storageBucket` | `"cloudinary"` |

## Delete

```java
cloudinaryService.delete(publicId, resourceType);
```

- Log warning nếu fail, không throw
- Chưa gọi khi soft-delete document — cân nhắc thêm async cleanup

## Task: Upload status tracking

Cloudinary upload hiện **đồng bộ** — client chờ đến khi xong.

### Option A — Client-side progress (đơn giản)
Frontend dùng `XMLHttpRequest.upload.onprogress` — **không cần backend**.

### Option B — Backend job tracking (phức tạp hơn)

1. Tạo entity `UploadJob`:
```java
@Entity
public class UploadJob {
    UUID id;
    UUID userId;
    String fileName;
    UploadStatus status;  // PENDING, UPLOADING, COMPLETED, FAILED
    UUID documentId;      // null until done
    String errorMessage;
    OffsetDateTime createdAt;
}
```

2. Flow:
   - `POST /upload/init` → tạo job, trả `jobId`
   - `POST /upload/{jobId}/file` → upload + update status
   - `GET /upload/{jobId}/status` → poll status

3. Hoặc dùng **SSE** push status thay vì polling.

**Khuyến nghị:** Bắt đầu Option A (FE only). Chỉ làm Option B nếu spec yêu cầu server-side tracking.

## Task: Preview file

### PDF & Image preview

Cloudinary hỗ trợ transformation URL:

```
# Image thumbnail
https://res.cloudinary.com/{cloud}/image/upload/w_800,h_600,c_limit/{public_id}

# PDF → image preview (trang 1)
https://res.cloudinary.com/{cloud}/image/upload/pg_1/{public_id}.jpg
```

### Endpoint gợi ý

```java
@GetMapping("/{id}/preview-url")
public ResponseEntity<ApiResponse<PreviewUrlResponse>> getPreviewUrl(
        @PathVariable UUID id,
        @AuthenticationPrincipal User user) {
    // check access → build transformation URL → return
}
```

Response:
```json
{
  "previewUrl": "https://res.cloudinary.com/...",
  "fileType": "application/pdf",
  "previewType": "image"  // hoặc "pdf" nếu trả raw URL
}
```

### File types preview

| MIME | Preview strategy |
|---|---|
| `image/jpeg`, `image/png` | Direct URL hoặc resize transform |
| `application/pdf` | Cloudinary pg_1 transform hoặc raw URL + react-pdf ở FE |
| Office docs | Không preview native — cần convert service hoặc chỉ download |

## Task: Download

Dùng `secure_url` từ DB. Cho private access:

```java
// Signed URL (nếu cần time-limited)
cloudinary.url().signed()
    .publicId(publicId)
    .resourceType("raw")
    .generate();
```

## Giới hạn

| Giới hạn | Giá trị |
|---|---|
| Max file size | 10 MB (app-level) |
| Folder | `documents/` |
| Allowed types | Xem `DocumentService.ALLOWED_MIME_TYPES` |

## Error handling

| ErrorCode | Khi nào |
|---|---|
| `UPLOAD_FAILED` | Cloudinary IOException |
| `FILE_TOO_LARGE` | > 10 MB |
| `INVALID_FILE_TYPE` | MIME không trong whitelist |
| `EMPTY_FILE` | File rỗng |
