# 🚀 RAG Implementation Guide - Step by Step

## ✅ Phase 1: Core Components (COMPLETED)

### Created Files:

1. **ChunkingService.java** - Interface chia văn bản
2. **ChunkingServiceImpl.java** - Implementation với 3 strategies
3. **TextChunk.java** - DTO cho chunks
4. **ChunkingStrategy.java** - Enum strategies
5. **DocumentChunk.java** - Entity lưu chunks + embeddings
6. **DocumentChunkRepository.java** - JPA repository

**Status:** ✅ Compiled successfully

---

## 📋 Phase 2: Embedding & Vector Store (TODO)

### Step 1: Create Embedding Service

**File:** `EmbeddingService.java`
```java
public interface EmbeddingService {
    float[] embed(String text);
    List<float[]> embedBatch(List<String> texts);
    String getModelName();
    int getDimensions();
}
```

**File:** `OpenAIEmbeddingService.java`
```java
@Service
@ConditionalOnProperty(name = "ai.embedding.provider", havingValue = "openai")
public class OpenAIEmbeddingService implements EmbeddingService {
    
    @Value("${ai.embedding.model:text-embedding-3-small}")
    private String model;
    
    @Value("${ai.api-key}")
    private String apiKey;
    
    private static final String EMBEDDING_URL = 
        "https://api.openai.com/v1/embeddings";
    
    @Override
    public float[] embed(String text) {
        // Call OpenAI Embeddings API
        // POST /v1/embeddings
        // Body: { "model": "text-embedding-3-small", "input": "text" }
    }
    
    @Override
    public int getDimensions() {
        return 1536; // text-embedding-3-small
    }
}
```

**Config:**
```properties
# application.properties
ai.embedding.provider=openai
ai.embedding.model=text-embedding-3-small
ai.embedding.dimensions=1536
ai.embedding.batch-size=100
```

---

### Step 2: Create Vector Store Service

**File:** `VectorStoreService.java`
```java
public interface VectorStoreService {
    void storeChunks(UUID documentId, List<ChunkWithEmbedding> chunks);
    List<ChunkWithScore> findSimilar(float[] queryEmbedding, UUID documentId, int topK);
    void deleteByDocumentId(UUID documentId);
    boolean isIndexed(UUID documentId);
}
```

**File:** `InMemoryVectorStoreService.java`
```java
@Service
public class InMemoryVectorStoreService implements VectorStoreService {
    
    private final DocumentChunkRepository chunkRepository;
    private final ObjectMapper objectMapper;
    
    @Override
    public void storeChunks(UUID documentId, List<ChunkWithEmbedding> chunks) {
        List<DocumentChunk> entities = chunks.stream()
            .map(this::toEntity)
            .collect(Collectors.toList());
        
        chunkRepository.saveAll(entities);
    }
    
    @Override
    public List<ChunkWithScore> findSimilar(
            float[] queryEmbedding, 
            UUID documentId, 
            int topK) {
        
        // 1. Load all chunks for document
        List<DocumentChunk> chunks = 
            chunkRepository.findByDocumentIdOrderByChunkIndexAsc(documentId);
        
        // 2. Calculate cosine similarity for each chunk
        List<ChunkWithScore> scored = chunks.stream()
            .map(chunk -> {
                float[] embedding = deserializeEmbedding(chunk.getEmbedding());
                float score = cosineSimilarity(queryEmbedding, embedding);
                return new ChunkWithScore(chunk, score);
            })
            .filter(cws -> cws.getScore() > 0.5f) // Threshold
            .sorted((a, b) -> Float.compare(b.getScore(), a.getScore()))
            .limit(topK)
            .collect(Collectors.toList());
        
        return scored;
    }
    
    private float cosineSimilarity(float[] a, float[] b) {
        float dotProduct = 0.0f;
        float normA = 0.0f;
        float normB = 0.0f;
        
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        
        return dotProduct / (float)(Math.sqrt(normA) * Math.sqrt(normB));
    }
    
    private String serializeEmbedding(float[] embedding) {
        try {
            return objectMapper.writeValueAsString(embedding);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize embedding", e);
        }
    }
    
    private float[] deserializeEmbedding(String json) {
        try {
            return objectMapper.readValue(json, float[].class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize embedding", e);
        }
    }
}
```

---

### Step 3: Create Document Indexing Service

**File:** `DocumentIndexingService.java`
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentIndexingService {
    
    private final DocumentRepository documentRepository;
    private final ChunkingService chunkingService;
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStore;
    
    @Async
    @Transactional
    public CompletableFuture<IndexingResult> indexDocument(UUID documentId) {
        log.info("Starting indexing for document {}", documentId);
        
        // 1. Load document
        Document doc = documentRepository.findById(documentId)
            .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND));
        
        if (!StringUtils.hasText(doc.getExtractedText())) {
            log.warn("Document {} has no extracted text", documentId);
            return CompletableFuture.completedFuture(
                IndexingResult.failed("No text content")
            );
        }
        
        try {
            // 2. Chunk text
            List<TextChunk> chunks = chunkingService.chunkText(
                doc.getExtractedText(),
                ChunkingStrategy.FIXED_SIZE
            );
            
            log.info("Created {} chunks for document {}", chunks.size(), documentId);
            
            // 3. Generate embeddings (batch)
            List<String> chunkTexts = chunks.stream()
                .map(TextChunk::getContent)
                .collect(Collectors.toList());
            
            List<float[]> embeddings = embeddingService.embedBatch(chunkTexts);
            
            // 4. Combine chunks + embeddings
            List<ChunkWithEmbedding> chunksWithEmbeddings = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                chunksWithEmbeddings.add(new ChunkWithEmbedding(
                    chunks.get(i),
                    embeddings.get(i)
                ));
            }
            
            // 5. Store in vector store
            vectorStore.storeChunks(documentId, chunksWithEmbeddings);
            
            // 6. Update document metadata
            doc.setIsIndexed(true);
            doc.setChunkCount(chunks.size());
            doc.setIndexedAt(OffsetDateTime.now());
            doc.setEmbeddingModel(embeddingService.getModelName());
            documentRepository.save(doc);
            
            log.info("Successfully indexed document {}", documentId);
            return CompletableFuture.completedFuture(
                IndexingResult.success(chunks.size())
            );
            
        } catch (Exception e) {
            log.error("Failed to index document {}", documentId, e);
            return CompletableFuture.completedFuture(
                IndexingResult.failed(e.getMessage())
            );
        }
    }
    
    @Transactional
    public void reindexDocument(UUID documentId) {
        // Delete existing chunks
        vectorStore.deleteByDocumentId(documentId);
        
        // Reset document index status
        documentRepository.findById(documentId).ifPresent(doc -> {
            doc.setIsIndexed(false);
            doc.setChunkCount(0);
            documentRepository.save(doc);
        });
        
        // Re-index
        indexDocument(documentId);
    }
}
```

---

## 📋 Phase 3: Enhanced RAG Service (TODO)

### Enhance RAGService

**File:** Update `RAGService.java`
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class EnhancedRAGService {
    
    private final VectorStoreService vectorStore;
    private final EmbeddingService embeddingService;
    private final DocumentRepository documentRepository;
    
    private static final int DEFAULT_TOP_K = 5;
    private static final float RELEVANCE_THRESHOLD = 0.7f;
    
    public RAGContext retrieveContext(UUID documentId, String question) {
        return retrieveContext(documentId, question, DEFAULT_TOP_K);
    }
    
    public RAGContext retrieveContext(UUID documentId, String question, int topK) {
        log.info("Retrieving context for document {} with question: {}", 
                 documentId, question);
        
        // 1. Check if document is indexed
        Document doc = documentRepository.findById(documentId)
            .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND));
        
        if (!Boolean.TRUE.equals(doc.getIsIndexed())) {
            throw new AppException(ErrorCode.DOCUMENT_NOT_INDEXED);
        }
        
        // 2. Embed question
        float[] queryEmbedding = embeddingService.embed(question);
        
        // 3. Find similar chunks
        List<ChunkWithScore> chunks = vectorStore.findSimilar(
            queryEmbedding, 
            documentId, 
            topK
        );
        
        // 4. Filter by relevance
        List<ChunkWithScore> relevant = chunks.stream()
            .filter(c -> c.getScore() >= RELEVANCE_THRESHOLD)
            .collect(Collectors.toList());
        
        log.info("Found {} relevant chunks (threshold: {})", 
                 relevant.size(), RELEVANCE_THRESHOLD);
        
        return RAGContext.builder()
            .question(question)
            .chunks(relevant)
            .documentId(documentId)
            .documentTitle(doc.getTitle())
            .build();
    }
    
    public String buildPrompt(RAGContext context) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("# System Instruction\n");
        prompt.append("Bạn là AI Study Assistant của AI Study Hub.\n");
        prompt.append("Trả lời câu hỏi dựa CHÍNH XÁC vào các đoạn tài liệu dưới đây.\n\n");
        
        prompt.append("# Tài liệu: ").append(context.getDocumentTitle()).append("\n\n");
        
        if (context.getChunks().isEmpty()) {
            prompt.append("⚠️ Không tìm thấy nội dung liên quan trong tài liệu.\n\n");
        } else {
            prompt.append("## Các đoạn liên quan:\n\n");
            
            for (int i = 0; i < context.getChunks().size(); i++) {
                ChunkWithScore chunk = context.getChunks().get(i);
                prompt.append(String.format("### [Đoạn %d] (Độ liên quan: %.1f%%)\n", 
                    i + 1, chunk.getScore() * 100));
                prompt.append(chunk.getContent()).append("\n\n");
            }
        }
        
        prompt.append("---\n\n");
        prompt.append("# Câu hỏi\n");
        prompt.append(context.getQuestion()).append("\n\n");
        
        prompt.append("# Hướng dẫn trả lời\n");
        prompt.append("1. Chỉ sử dụng thông tin từ các đoạn tài liệu trên\n");
        prompt.append("2. Trích dẫn [Đoạn X] khi reference\n");
        prompt.append("3. Nếu không có thông tin, nói rõ 'Tài liệu không đề cập đến điều này'\n");
        prompt.append("4. Trả lời ngắn gọn, rõ ràng, dễ hiểu\n");
        prompt.append("5. Sử dụng cùng ngôn ngữ với câu hỏi\n\n");
        
        prompt.append("# Trả lời\n");
        
        return prompt.toString();
    }
}
```

---

## 📋 Phase 4: API Endpoints (TODO)

### Update DocumentController

Add indexing endpoints:

```java
@PostMapping("/{id}/index")
public ResponseEntity<ApiResponse<IndexingStatusResponse>> indexDocument(
        @PathVariable UUID id,
        @AuthenticationPrincipal User currentUser) {
    
    // Check ownership
    Document doc = documentRepository.findById(id)
        .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND));
    
    if (!doc.getUserId().equals(currentUser.getId())) {
        throw new AppException(ErrorCode.FORBIDDEN_ACCESS);
    }
    
    // Start async indexing
    indexingService.indexDocument(id);
    
    return ResponseEntity.accepted().body(
        ApiResponse.success(
            IndexingStatusResponse.builder()
                .documentId(id)
                .status("INDEXING")
                .message("Đang xử lý tài liệu để hỗ trợ hỏi đáp AI")
                .build()
        )
    );
}

@GetMapping("/{id}/index-status")
public ResponseEntity<ApiResponse<IndexingStatusResponse>> getIndexStatus(
        @PathVariable UUID id,
        @AuthenticationPrincipal User currentUser) {
    
    Document doc = documentRepository.findById(id)
        .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND));
    
    if (!doc.getUserId().equals(currentUser.getId())) {
        throw new AppException(ErrorCode.FORBIDDEN_ACCESS);
    }
    
    IndexingStatusResponse status = IndexingStatusResponse.builder()
        .documentId(id)
        .isIndexed(doc.getIsIndexed())
        .chunkCount(doc.getChunkCount())
        .embeddingModel(doc.getEmbeddingModel())
        .indexedAt(doc.getIndexedAt())
        .status(Boolean.TRUE.equals(doc.getIsIndexed()) ? "INDEXED" : "NOT_INDEXED")
        .build();
    
    return ResponseEntity.ok(ApiResponse.success(status));
}
```

### Enhance ChatService

Add RAG-powered chat:

```java
@Override
public ChatResponse chatWithDocument(
        UUID documentId, 
        DocumentChatRequest request, 
        UUID userId) {
    
    // 1. Verify document access
    Document doc = documentRepository.findById(documentId)
        .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND));
    
    if (!doc.getUserId().equals(userId)) {
        throw new AppException(ErrorCode.FORBIDDEN_ACCESS);
    }
    
    // 2. Check if indexed
    if (!Boolean.TRUE.equals(doc.getIsIndexed())) {
        throw new AppException(ErrorCode.DOCUMENT_NOT_INDEXED);
    }
    
    // 3. Get or create session
    ChatSession session = getOrCreateDocumentSession(
        request.getSessionId(),
        userId,
        documentId,
        request.getMessage()
    );
    
    // 4. Save user message
    saveMessage(session, MessageSender.USER, request.getMessage());
    
    // 5. Retrieve relevant context using RAG
    RAGContext context = ragService.retrieveContext(
        documentId,
        request.getMessage(),
        request.getTopK() != null ? request.getTopK() : 5
    );
    
    // 6. Build augmented prompt
    String prompt = ragService.buildPrompt(context);
    
    // 7. Generate answer
    String answer = aiService.generateAnswer(prompt);
    
    // 8. Save AI response with source metadata
    ChatMessage aiMessage = saveMessage(session, MessageSender.ASSISTANT, answer);
    
    // Store source chunks as JSON
    String sourcesJson = serializeSources(context.getChunks());
    aiMessage.setSourceChunks(sourcesJson);
    chatMessageRepository.save(aiMessage);
    
    // 9. Build response
    return ChatResponse.builder()
        .message(answer)
        .sessionId(session.getId())
        .messageId(aiMessage.getId())
        .sources(buildSourcesList(context.getChunks()))
        .timestamp(OffsetDateTime.now())
        .build();
}
```

---

## 🗄️ Database Migrations

### Add columns to documents table

```sql
ALTER TABLE documents
ADD is_indexed BIT DEFAULT 0;

ALTER TABLE documents
ADD chunk_count INT DEFAULT 0;

ALTER TABLE documents
ADD indexed_at DATETIME2;

ALTER TABLE documents
ADD embedding_model VARCHAR(100);
```

### Create document_chunks table

```sql
CREATE TABLE document_chunks (
    id UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(),
    document_id UNIQUEIDENTIFIER NOT NULL,
    chunk_index INT NOT NULL,
    content NVARCHAR(MAX) NOT NULL,
    embedding NVARCHAR(MAX),
    token_count INT,
    start_position INT,
    end_position INT,
    created_at DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE
);

CREATE INDEX idx_document_chunks_doc_id ON document_chunks(document_id);
CREATE INDEX idx_document_chunks_doc_chunk ON document_chunks(document_id, chunk_index);
```

---

## 🧪 Testing

### Test Chunking

```java
@Test
void testChunking() {
    String text = "This is a long document... " + 
                  "It has multiple sentences... " +
                  "We want to chunk it...";
    
    List<TextChunk> chunks = chunkingService.chunkText(text);
    
    assertThat(chunks).isNotEmpty();
    assertThat(chunks.get(0).getChunkIndex()).isEqualTo(0);
    assertThat(chunks.get(0).getContent()).isNotEmpty();
}
```

### Test RAG Flow

```bash
# 1. Upload document
curl -X POST http://localhost:8081/api/v1/documents/upload \
  -H "Authorization: Bearer {JWT}" \
  -F "file=@test.pdf" \
  -F 'request={"title":"Test Doc"};type=application/json'

# 2. Index document
curl -X POST http://localhost:8081/api/v1/documents/{id}/index \
  -H "Authorization: Bearer {JWT}"

# 3. Check index status
curl -X GET http://localhost:8081/api/v1/documents/{id}/index-status \
  -H "Authorization: Bearer {JWT}"

# 4. Ask question
curl -X POST http://localhost:8081/api/v1/chat/document/{id} \
  -H "Authorization: Bearer {JWT}" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "What is the main topic?",
    "topK": 5
  }'
```

---

## 📊 Summary

### Completed ✅
- Chunking system (3 strategies)
- Document chunk entity & repository
- Database schema design

### Next Steps 📋
1. Implement EmbeddingService (OpenAI/Gemini)
2. Implement VectorStoreService (in-memory)
3. Create DocumentIndexingService (async)
4. Enhance RAGService with vector retrieval
5. Add API endpoints for indexing & Q&A
6. Test end-to-end flow

### Estimated Time ⏱️
- Phase 2 (Embedding & Vector): 2-3 days
- Phase 3 (Enhanced RAG): 1-2 days
- Phase 4 (API Integration): 1-2 days
- Testing & Polish: 1-2 days

**Total: ~1 week for MVP RAG system** 🚀
