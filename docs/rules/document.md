# Document Module

> Quy tắc phát triển tính năng **Document** trên backend.

## Vị trí code

```
feature/document/
  controller/DocumentController.java
  service/DocumentService.java
  service/CloudinaryService.java
  entity/Document.java
  repository/DocumentRepository.java
  dto/request/UploadDocumentRequest.java
  dto/response/DocumentResponse.java
```

## Entity hiện tại

Bảng `documents`:

| Column | Type | Ghi chú |
|---|---|---|
| `id` | UUID | PK |
| `uploaded_by` | UUID | FK → users |
| `title` | varchar(255) | Required |
| `description` | varchar(1000) | Optional |
| `file_url` | varchar(2000) | Cloudinary secure URL |
| `file_name` | varchar(255) | |
| `original_file_name` | varchar(255) | |
| `file_size` | bigint | bytes |
| `file_type` | varchar(100) | MIME type |
| `storage_public_id` | varchar(500) | Cloudinary public_id |
| `storage_key` | varchar(500) | = public_id |
| `storage_bucket` | varchar | = `"cloudinary"` |
| `is_public` | boolean | default true |
| `created_at`, `updated_at` | OffsetDateTime | Auto |
| `deleted_at` | OffsetDateTime | Soft delete |

### Field cần thêm (task)

| Column | Type | Task |
|---|---|---|
| `subject` | varchar(100) | Lọc theo môn học |

Thêm vào entity + migration tự động qua `ddl-auto=update`. Cập nhật DTO request/response.

## Endpoints đã có

| Method | Path | Service method |
|---|---|---|
| POST | `/upload` | `upload(file, request, userId)` |
| GET | `/my` | `getMyDocuments(userId)` |
| GET | `/{id}` | `getById(id, userId)` |
| DELETE | `/{id}` | `deleteById(id, userId)` |

## Task: Download

### Yêu cầu
- Chỉ owner hoặc user có quyền xem doc mới download được
- Trả file đúng tên gốc và content-type

### Implementation gợi ý

```java
// DocumentService
public DownloadInfo getDownloadInfo(UUID docId, UUID userId) {
    Document doc = findOrThrow(docId);
    checkAccess(doc, userId);
    return new DownloadInfo(doc.getFileUrl(), doc.getOriginalFileName(), doc.getFileType());
}
```

Option 1: Redirect tới Cloudinary URL (đơn giản, doc public trên Cloudinary)
Option 2: Proxy stream qua backend (kiểm soát tốt hơn, tốn bandwidth)

Thêm `ErrorCode` nếu cần: `DOWNLOAD_FAILED`

## Task: Update metadata

### Request DTO

```java
// dto/request/UpdateDocumentRequest.java
public class UpdateDocumentRequest {
    @NotBlank @Size(max = 255) private String title;
    @Size(max = 1000) private String description;
    @Size(max = 100) private String subject;
    private Boolean isPublic;  // optional
}
```

### Service

```java
@Transactional
public DocumentResponse update(UUID id, UpdateDocumentRequest req, UUID userId) {
    Document doc = findOwnedOrThrow(id, userId);
    doc.setTitle(req.getTitle().trim());
    doc.setDescription(req.getDescription());
    doc.setSubject(req.getSubject());
    if (req.getIsPublic() != null) doc.setPublic(req.getIsPublic());
    return toResponse(documentRepository.save(doc));
}
```

### Endpoint

```
PATCH /api/v1/documents/{id}
```

## Task: Search

### Repository

```java
@Query("""
    SELECT d FROM Document d
    WHERE d.deletedAt IS NULL
      AND d.userId = :userId
      AND (LOWER(d.title) LIKE LOWER(CONCAT('%', :q, '%'))
        OR LOWER(d.description) LIKE LOWER(CONCAT('%', :q, '%')))
    ORDER BY d.createdAt DESC
    """)
Page<Document> searchByUser(@Param("userId") UUID userId,
                              @Param("q") String q,
                              Pageable pageable);
```

### Endpoint

```
GET /api/v1/documents/search?q=keyword&page=0&size=20
```

## Task: Filter by subject

Gộp vào search hoặc endpoint riêng:

```java
Page<Document> findByUserIdAndSubjectAndDeletedAtIsNull(
    UUID userId, String subject, Pageable pageable);
```

Query kết hợp:
```
GET /api/v1/documents/search?q=...&subject=SWP391
```

## File validation (upload)

Giữ nguyên logic trong `DocumentService.validateFile()`:

- Max 10 MB
- MIME: PDF, DOC/DOCX, XLS/XLSX, PPT/PPTX, TXT, JPEG, PNG

## Soft delete

- Set `deletedAt = OffsetDateTime.now()`
- **Không** xóa file Cloudinary ngay (có thể thêm cleanup job sau)
- `CloudinaryService.delete()` đã có nhưng chưa được gọi

## DocumentResponse

Giữ shape hiện tại, thêm field khi cần:

```java
.id, .title, .description, .subject  // mới
.fileUrl, .fileName, .fileSize, .fileType
.isPublic, .createdAt
```

**Lưu ý:** `fileUrl` trả về cho frontend preview — cân nhắc không trả cho doc private nếu cần bảo mật (thay bằng signed URL endpoint).
