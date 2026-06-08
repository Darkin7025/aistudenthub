# AI Chatbot Module

> Quy tắc phát triển **AI Chatbot** — module mới, chưa có code.

## Trạng thái

❌ Chưa implement. Tạo module mới theo pattern `feature/chatbot/`.

## Package structure (đề xuất)

```
feature/chatbot/
  controller/
    ChatController.java          # Conversations, messages
    DocumentChatController.java    # RAG ask about document
  service/
    ChatService.java             # Orchestration
    LlmService.java              # Gọi OpenAI/Gemini API
    RagService.java              # Retrieval-augmented generation
    DocumentIngestionService.java  # Parse & embed documents
    ConversationService.java     # CRUD conversations
  entity/
    Conversation.java
    ChatMessage.java
    DocumentChunk.java           # RAG embeddings
  repository/
    ConversationRepository.java
    ChatMessageRepository.java
    DocumentChunkRepository.java
  dto/
    request/SendMessageRequest.java
    request/AskDocumentRequest.java
    response/ChatMessageResponse.java
    response/ConversationResponse.java
```

## Database entities (đề xuất)

### conversations

| Column | Type |
|---|---|
| id | UUID PK |
| user_id | UUID FK |
| title | varchar(255) — auto từ message đầu |
| document_id | UUID nullable — nếu chat trong context 1 doc |
| created_at, updated_at | OffsetDateTime |

### chat_messages

| Column | Type |
|---|---|
| id | UUID PK |
| conversation_id | UUID FK |
| role | enum: USER, ASSISTANT, SYSTEM |
| content | text/nvarchar(max) |
| token_count | int nullable |
| created_at | OffsetDateTime |

### document_chunks (RAG)

| Column | Type |
|---|---|
| id | UUID PK |
| document_id | UUID FK |
| chunk_index | int |
| content | text |
| embedding | varbinary hoặc JSON — tùy vector store |
| created_at | OffsetDateTime |

## Task: Chat với chatbot

### Flow

```
1. User gửi message
2. Lưu ChatMessage (role=USER)
3. Lấy history N messages gần nhất
4. Gọi LlmService.chat(messages)
5. Lưu ChatMessage (role=ASSISTANT)
6. Trả response
```

### Endpoint

```
POST /api/v1/chat/conversations
Body: { "title": "optional", "documentId": "optional-uuid" }
→ ConversationResponse

POST /api/v1/chat/conversations/{id}/messages
Body: { "content": "Xin chào" }
→ ChatMessageResponse
```

### LLM integration

Config qua env:
```properties
ai.provider=openai          # hoặc gemini
ai.api-key=${AI_API_KEY}
ai.model=gpt-4o-mini
ai.max-tokens=2048
```

Tạo `LlmService` interface + implementation — không gọi API trực tiếp trong controller.

```java
public interface LlmService {
    String chat(List<ChatMessage> history, String userMessage);
    Flux<String> chatStream(List<ChatMessage> history, String userMessage);
}
```

## Task: Hỏi đáp về tài liệu (RAG)

### Pipeline

```
1. User chọn document + đặt câu hỏi
2. RagService.retrieve(documentId, question) → top-K chunks
3. Build prompt: system + context chunks + question
4. LlmService.chat() → answer
5. Lưu message kèm metadata (source chunks)
```

### Document ingestion (khi upload hoặc on-demand)

```
1. Download file từ Cloudinary
2. Parse text (PDFBox cho PDF, plain text cho TXT)
3. Chunk text (500-1000 tokens, overlap 100)
4. Generate embeddings qua LLM API
5. Lưu DocumentChunk
```

### Endpoint

```
POST /api/v1/chat/documents/{documentId}/ask
Body: { "question": "Chương 3 nói về gì?", "conversationId": "optional" }
→ { "answer": "...", "sources": [{ "chunkIndex": 2, "excerpt": "..." }] }
```

### Vector search options

| Option | Độ phức tạp | Ghi chú |
|---|---|---|
| In-memory cosine similarity | Thấp | OK cho demo, < 1000 chunks |
| SQL Server vector (2025+) | Trung bình | Nếu DB hỗ trợ |
| Pinecone / Qdrant | Cao | Production scale |

**Khuyến nghị SWP391:** In-memory hoặc simple SQL LIKE fallback cho MVP.

## Task: Streaming response

### SSE với SseEmitter (Spring MVC)

```java
@PostMapping(value = "/{id}/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter streamMessage(
        @PathVariable UUID id,
        @RequestBody SendMessageRequest request,
        @AuthenticationPrincipal User user) {

    SseEmitter emitter = new SseEmitter(60_000L);
    chatService.streamResponse(id, request, user.getId(), emitter);
    return emitter;
}
```

### Event format

```
event: token
data: {"content":"Hello"}

event: token
data: {"content":" world"}

event: done
data: {"messageId":"uuid","conversationId":"uuid"}
```

### Error mid-stream

```
event: error
data: {"message":"Lỗi kết nối AI, vui lòng thử lại"}
```

## Task: Lịch sử chat

### Endpoints

```
GET /api/v1/chat/conversations?page=0&size=20
→ Page<ConversationResponse>

GET /api/v1/chat/conversations/{id}/messages?page=0&size=50
→ Page<ChatMessageResponse>

DELETE /api/v1/chat/conversations/{id}
→ soft delete hoặc hard delete
```

### Rules

- User chỉ truy cập conversation của mình (`user_id` check)
- Sort messages by `created_at ASC`
- Pagination mặc định size=20

## Error codes cần thêm

```java
CONVERSATION_NOT_FOUND("Cuộc hội thoại không tồn tại", NOT_FOUND),
AI_SERVICE_UNAVAILABLE("Dịch vụ AI tạm thời không khả dụng", SERVICE_UNAVAILABLE),
DOCUMENT_NOT_INDEXED("Tài liệu chưa được xử lý cho hỏi đáp", BAD_REQUEST),
```

## Security

- Tất cả endpoint `/api/v1/chat/**` cần JWT
- Không gửi API key AI ra client
- Rate limit (khuyến nghị): 20 messages/phút/user
- Không log full prompt chứa PII

## Dependencies đề xuất

```xml
<!-- Chọn 1 LLM SDK -->
<dependency>
    <groupId>com.theokanning.openai-gpt3-java</groupId>
    <artifactId>service</artifactId>
    <version>0.18.2</version>
</dependency>

<!-- PDF parsing cho RAG -->
<dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>pdfbox</artifactId>
    <version>3.0.3</version>
</dependency>
```

Hoặc **Spring AI** nếu team muốn abstraction đa provider.
