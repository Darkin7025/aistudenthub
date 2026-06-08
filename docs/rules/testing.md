# Testing Conventions

> Quy tắc test cho **aistudenthub** backend.

## Test structure

```
src/test/java/com/example/swp391/aistudenthub/
  AistudenthubApplicationTests.java    # Context load
  feature/
    auth/AuthServiceTest.java
    document/DocumentServiceTest.java
    chatbot/ChatServiceTest.java

src/test/resources/
  auth-test.http                       # Manual REST Client tests
  document-test.http                   # (tạo mới)
```

## Manual API testing (.http files)

Dùng IntelliJ HTTP Client hoặc VS Code REST Client.

### Pattern

```http
### Login
POST http://localhost:8080/api/v1/auth/login
Content-Type: application/json

{
  "email": "test@example.com",
  "password": "password123"
}

> {%
  client.global.set("token", response.body.data.token);
%}

### Get my documents
GET http://localhost:8080/api/v1/documents/my
Authorization: Bearer {{token}}
```

Tạo `document-test.http` khi thêm endpoint document mới.

## Unit tests

### Service test template

```java
@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock DocumentRepository documentRepository;
    @Mock CloudinaryService cloudinaryService;
    @InjectMocks DocumentService documentService;

    @Test
    void getById_notFound_throwsAppException() {
        when(documentRepository.findByIdAndDeletedAtIsNull(any()))
            .thenReturn(Optional.empty());

        assertThrows(AppException.class,
            () -> documentService.getById(UUID.randomUUID(), UUID.randomUUID()));
    }
}
```

### Test cases ưu tiên

| Feature | Test |
|---|---|
| Upload | Valid file, empty file, oversized, invalid MIME |
| Get | Found, not found, forbidden (private) |
| Update | Owner OK, non-owner 403 |
| Search | Match title, match description, no results |
| Delete | Soft delete sets deletedAt |
| Chat | Save message, stream emits events |

## Integration tests

```java
@SpringBootTest
@AutoConfigureMockMvc
class DocumentControllerIntegrationTest {

    @Autowired MockMvc mockMvc;

    @Test
    void upload_withoutToken_returns401() throws Exception {
        mockMvc.perform(multipart("/api/v1/documents/upload"))
            .andExpect(status().isUnauthorized());
    }
}
```

Dùng `@Testcontainers` cho SQL Server nếu cần CI — chưa setup hiện tại.

## Swagger testing

1. Start app: `./mvnw spring-boot:run`
2. Mở `http://localhost:8080/swagger-ui.html`
3. Authorize với Bearer token từ login
4. Test endpoint mới

## Checklist trước khi hoàn thành task

- [ ] Endpoint hoạt động qua Swagger hoặc .http file
- [ ] Error cases trả đúng ErrorCode + HTTP status
- [ ] Ownership check tested (403 case)
- [ ] Unit test cho business logic phức tạp (nếu có)
- [ ] Không break existing `auth-test.http` flows
