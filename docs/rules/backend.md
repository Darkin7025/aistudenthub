# Backend Coding Standards

> Quy chuẩn Java/Spring Boot cho repository **aistudenthub**.

## Package & naming

```
feature/{module}/
  controller/   XxxController.java
  service/      XxxService.java
  repository/   XxxRepository.java
  entity/       Xxx.java
  dto/
    request/    CreateXxxRequest.java
    response/   XxxResponse.java
```

| Loại | Convention | Ví dụ |
|---|---|---|
| Controller | `{Feature}Controller` | `DocumentController` |
| Service | `{Feature}Service` | `DocumentService` |
| Repository | `{Feature}Repository` | `DocumentRepository` |
| Request DTO | `{Action}{Feature}Request` | `UploadDocumentRequest` |
| Response DTO | `{Feature}Response` | `DocumentResponse` |

## Lombok

Dùng nhất quán như code hiện có:

```java
@RequiredArgsConstructor  // constructor injection
@Slf4j                    // logging
@Builder @Getter @Setter @NoArgsConstructor @AllArgsConstructor  // entity/DTO
```

Không dùng `@Autowired` field injection.

## Controller template

```java
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Tag(name = "Documents")
public class DocumentController {

    private final DocumentService documentService;

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết tài liệu")
    public ResponseEntity<ApiResponse<DocumentResponse>> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(
            ApiResponse.success(documentService.getById(id, currentUser.getId())));
    }
}
```

## Service template

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    @Transactional(readOnly = true)
    public DocumentResponse getById(UUID id, UUID userId) { ... }

    @Transactional
    public DocumentResponse update(UUID id, UpdateRequest req, UUID userId) { ... }
}
```

- **Read:** `@Transactional(readOnly = true)`
- **Write:** `@Transactional` (default read-write)
- Throw `AppException`, không return null cho not-found

## Error handling

```java
// Trong service
throw new AppException(ErrorCode.DOCUMENT_NOT_FOUND);

// Thêm ErrorCode mới trong exception/ErrorCode.java
DOCUMENT_UPDATE_FAILED("Không thể cập nhật tài liệu", HttpStatus.BAD_REQUEST),
```

`GlobalExceptionHandler` đã xử lý `AppException` → `ApiResponse.error(message)` + đúng HTTP status.

## Validation

Request DTO dùng Jakarta Validation:

```java
public class UpdateDocumentRequest {
    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 255)
    private String title;

    @Size(max = 1000)
    private String description;
}
```

Controller: `@Valid @RequestBody UpdateDocumentRequest request`

## Logging

```java
log.info("Document saved: id={}, user={}", saved.getId(), userId);
log.warn("Cloudinary delete failed for publicId={}: {}", publicId, e.getMessage());
log.error("Upload failed", e);  // kèm exception object
```

Không log password, token, API secret.

## Dependency thêm mới

Thêm vào `pom.xml`, ưu tiên Spring ecosystem:

| Nhu cầu | Dependency gợi ý |
|---|---|
| SSE streaming | `spring-boot-starter-webflux` (hoặc SseEmitter trong MVC) |
| OpenAI | `com.theokanning.openai-gpt3-java` hoặc Spring AI |
| PDF parsing (RAG) | Apache PDFBox |
| Full-text search | Spring Data JPA Specification hoặc SQL Server CONTAINS |

Ghi chú lý do thêm dependency trong commit message.

## Comment

- Javadoc tiếng Việt cho public API method
- Inline comment chỉ cho logic không hiển nhiên
- Không comment code đã self-explanatory
