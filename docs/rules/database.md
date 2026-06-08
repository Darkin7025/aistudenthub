# Database Conventions

> Quy tắc SQL Server + JPA/Hibernate cho **aistudenthub**.

## Connection

```properties
spring.datasource.url=jdbc:sqlserver://localhost:1444;databaseName=ai_study_hub;...
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.SQLServerDialect
```

Local credentials: `application-local.properties` (gitignored).

## Naming

| Loại | Convention | Ví dụ |
|---|---|---|
| Table | snake_case, plural | `documents`, `chat_messages` |
| Column | snake_case | `uploaded_by`, `created_at` |
| Entity field | camelCase | `userId`, `createdAt` |
| FK column | `{entity}_id` hoặc mô tả | `uploaded_by`, `conversation_id` |

## ID strategy

```java
@Id
@GeneratedValue(strategy = GenerationType.UUID)
@Column(columnDefinition = "UNIQUEIDENTIFIER")
private UUID id;
```

Dùng UUID cho mọi bảng mới.

## Timestamps

```java
@Column(name = "created_at", nullable = false, updatable = false)
private OffsetDateTime createdAt;

@Column(name = "updated_at", nullable = false)
private OffsetDateTime updatedAt;

@PrePersist
protected void onCreate() {
    createdAt = OffsetDateTime.now();
    updatedAt = OffsetDateTime.now();
}

@PreUpdate
protected void onUpdate() {
    updatedAt = OffsetDateTime.now();
}
```

Timezone: UTC (`hibernate.jdbc.time_zone=UTC`).

## Soft delete

```java
@Column(name = "deleted_at")
private OffsetDateTime deletedAt;
```

Repository queries **luôn** filter:
```java
findByUserIdAndDeletedAtIsNull(UUID userId)
findByIdAndDeletedAtIsNull(UUID id)
```

## Tables hiện có

| Table | Entity |
|---|---|
| `users` | `User` |
| `refresh_tokens` | `RefreshToken` |
| `password_reset_tokens` | `PasswordResetToken` |
| `documents` | `Document` |

## Tables cần tạo (chatbot)

| Table | Mục đích |
|---|---|
| `conversations` | Chat sessions |
| `chat_messages` | Messages trong conversation |
| `document_chunks` | RAG text chunks + embeddings |
| `upload_jobs` | (optional) Upload status tracking |

## Index recommendations

Khi thêm field/query mới, cân nhắc index:

```sql
-- Document search
CREATE INDEX idx_documents_user_deleted ON documents(uploaded_by, deleted_at);
CREATE INDEX idx_documents_subject ON documents(subject) WHERE deleted_at IS NULL;

-- Chat history
CREATE INDEX idx_messages_conversation ON chat_messages(conversation_id, created_at);
CREATE INDEX idx_conversations_user ON conversations(user_id, created_at DESC);
```

Với `ddl-auto=update`, tạo index qua:
- `@Table(indexes = @Index(...))` trên entity, hoặc
- Migration script thủ công cho production

## Query patterns

### Pagination
```java
Page<Document> findByUserIdAndDeletedAtIsNull(UUID userId, Pageable pageable);
```

### Search
```java
@Query("SELECT d FROM Document d WHERE ... LIKE ...")
Page<Document> search(..., Pageable pageable);
```

### Specification (alternative)
```java
public interface DocumentRepository extends JpaRepository<Document, UUID>,
                                            JpaSpecificationExecutor<Document> {}
```

Dùng `Specification` khi combine nhiều filter động (search + subject + date range).

## Migration strategy

| Môi trường | Strategy |
|---|---|
| Dev | `ddl-auto=update` — tự sync schema |
| Prod | Flyway/Liquibase (chưa setup) — nên thêm khi deploy |

Khi thêm column `subject` vào `Document`:
1. Thêm field entity
2. Hibernate auto-add column
3. Existing rows: `subject = NULL` (OK)

## SQL Server specifics

- UUID column: `UNIQUEIDENTIFIER`
- Long text: `NVARCHAR(MAX)` cho chat message content
- Không dùng `TEXT` deprecated type
- `trustServerCertificate=true` cho local dev

## Transaction boundaries

- Service layer owns `@Transactional`
- Repository không có business logic
- Read-only cho query: `@Transactional(readOnly = true)`
- Không gọi external API (Cloudinary, LLM) trong transaction dài — tách ra
