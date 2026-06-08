# 🎯 RAG System - Executive Summary

**Date:** 2026-06-06  
**Feature:** AI Chatbot - Hỏi đáp về tài liệu (Document Q&A with RAG)

---

## 📝 What is RAG?

**RAG = Retrieval-Augmented Generation**

Instead of asking AI blindly, RAG:
1. **Retrieves** relevant chunks from your document
2. **Augments** the prompt with those chunks
3. **Generates** answer based on actual document content

**Result:** Accurate answers grounded in your documents, not hallucinated!

---

## 🏗️ System Design

```
┌─────────────────────────────────────────────────┐
│              UPLOAD & INDEX                     │
├─────────────────────────────────────────────────┤
│  PDF Upload                                     │
│    ↓                                            │
│  Extract Text (PDFBox) ← Already implemented    │
│    ↓                                            │
│  Chunk Text (800 tokens, 150 overlap)   NEW    │
│    ↓                                            │
│  Generate Embeddings (OpenAI API)       NEW    │
│    ↓                                            │
│  Store in Vector DB                     NEW    │
└─────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────┐
│              QUESTION & ANSWER                  │
├─────────────────────────────────────────────────┤
│  User Question: "Chương 3 nói về gì?"           │
│    ↓                                            │
│  Embed Question (OpenAI API)            NEW    │
│    ↓                                            │
│  Find Similar Chunks (Cosine)           NEW    │
│    ↓                                            │
│  Build Prompt with Context              NEW    │
│    ↓                                            │
│  Generate Answer (AI API)  ← Already have       │
│    ↓                                            │
│  Return Answer + Sources                NEW    │
└─────────────────────────────────────────────────┘
```

---

## ✅ What's Been Implemented (Phase 1)

### Core Components ✅

1. **ChunkingService** - Chia văn bản thành chunks
   - Fixed size strategy (800 tokens, 150 overlap)
   - Paragraph strategy
   - Sentence strategy
   - Sentence-boundary aware (không cắt giữa câu)

2. **TextChunk DTO** - Data model cho chunks
   - content, startPosition, endPosition
   - tokenCount, chunkIndex

3. **DocumentChunk Entity** - Database entity
   - Lưu chunks + embeddings
   - Indexes for fast lookup
   - JSON embeddings format

4. **DocumentChunkRepository** - JPA repository
   - findByDocumentId
   - countByDocumentId
   - deleteByDocumentId

**Status:** ✅ Compiled successfully

---

## 📋 What Needs To Be Done (Phase 2-4)

### Phase 2: Embedding & Vector Store

**Priority:** HIGH  
**Time:** 2-3 days

**Components:**
- `EmbeddingService` - Call OpenAI/Gemini embedding API
- `OpenAIEmbeddingService` - Implementation cho OpenAI
- `GeminiEmbeddingService` - Implementation cho Gemini
- `VectorStoreService` - Store và retrieve embeddings
- `InMemoryVectorStoreService` - MVP implementation với cosine similarity

**Deliverables:**
- [ ] Embedding service cho OpenAI (text-embedding-3-small)
- [ ] Vector store với cosine similarity search
- [ ] Serialization/deserialization của embeddings (JSON)

---

### Phase 3: Document Indexing

**Priority:** HIGH  
**Time:** 1-2 days

**Components:**
- `DocumentIndexingService` - Async indexing pipeline
- Update `Document` entity (add is_indexed, chunk_count fields)
- Database migrations

**Flow:**
```
Upload PDF → Extract Text → Chunk → Embed → Store
```

**Deliverables:**
- [ ] Async indexing service
- [ ] Database migrations
- [ ] Index status tracking

---

### Phase 4: RAG Query

**Priority:** HIGH  
**Time:** 1-2 days

**Components:**
- Enhanced `RAGService` với vector retrieval
- `RAGContext` DTO
- `ChunkWithScore` DTO
- Update `ChatService` để dùng RAG

**Flow:**
```
Question → Embed → Find Similar → Build Prompt → Generate Answer
```

**Deliverables:**
- [ ] Vector search với relevance threshold
- [ ] Context-aware prompt building
- [ ] Source attribution (trả về chunks được dùng)

---

### Phase 5: API Integration

**Priority:** MEDIUM  
**Time:** 1-2 days

**Endpoints:**
- `POST /api/v1/documents/{id}/index` - Index document
- `GET /api/v1/documents/{id}/index-status` - Check status
- `POST /api/v1/chat/document/{id}` - Ask with RAG (enhance existing)
- `POST /api/v1/chat/document/{id}/stream` - Streaming RAG

**Deliverables:**
- [ ] Indexing API
- [ ] Enhanced chat API with sources
- [ ] Status checking endpoint

---

## 🗄️ Database Changes Needed

### Add columns to `documents` table

```sql
ALTER TABLE documents 
ADD is_indexed BIT DEFAULT 0,
    chunk_count INT DEFAULT 0,
    indexed_at DATETIME2,
    embedding_model VARCHAR(100);
```

### Create `document_chunks` table

```sql
CREATE TABLE document_chunks (
    id UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(),
    document_id UNIQUEIDENTIFIER NOT NULL,
    chunk_index INT NOT NULL,
    content NVARCHAR(MAX) NOT NULL,
    embedding NVARCHAR(MAX), -- JSON array
    token_count INT,
    start_position INT,
    end_position INT,
    created_at DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE
);

CREATE INDEX idx_document_chunks_doc_id ON document_chunks(document_id);
```

---

## 💰 Cost Estimation

### OpenAI Pricing (2026)

| Operation | Model | Cost per 1M tokens | Example Cost |
|-----------|-------|-------------------|--------------|
| **Embedding** | text-embedding-3-small | $0.02 | 100-page PDF (~50K tokens) = $0.001 |
| **Chat** | gpt-4o-mini | $0.15 input + $0.60 output | 1 question (~2K tokens) = $0.0005 |

**Monthly Estimate (1000 users):**
- Each user uploads 10 documents = 10,000 docs × $0.001 = **$10**
- Each user asks 100 questions = 100,000 × $0.0005 = **$50**
- **Total: ~$60/month**

Very affordable! 🎉

---

## 📊 API Flow Example

### 1. Upload & Index

```bash
# Upload
POST /api/v1/documents/upload
→ { documentId, extractedText, ... }

# Index (async)
POST /api/v1/documents/{id}/index
→ { status: "INDEXING", estimatedTime: "30s" }

# Check status
GET /api/v1/documents/{id}/index-status
→ { isIndexed: true, chunkCount: 25, embeddingModel: "text-embedding-3-small" }
```

### 2. Ask Question

```bash
POST /api/v1/chat/document/{documentId}
{
  "message": "Chương 3 nói về gì?",
  "topK": 5
}

Response:
{
  "answer": "Chương 3 trình bày về...",
  "sources": [
    {
      "chunkIndex": 5,
      "content": "Excerpt...",
      "score": 0.89,
      "citation": "[Đoạn 1]"
    }
  ],
  "sessionId": "uuid",
  "messageId": "uuid"
}
```

---

## 🎨 Frontend UI Mockup

```
┌────────────────────────────────────────────┐
│  📄 Document: Machine Learning Basics      │
│  Status: ✅ Indexed (25 chunks)            │
│  [Ask AI about this document]              │
└────────────────────────────────────────────┘

┌────────────────────────────────────────────┐
│  💬 Chat                                   │
├────────────────────────────────────────────┤
│  You: Chương 3 nói về gì?                  │
│                                            │
│  AI: Chương 3 trình bày về Neural         │
│  Networks, bao gồm:                        │
│  - Perceptron model                        │
│  - Activation functions                    │
│  - Backpropagation                         │
│                                            │
│  📌 Sources:                               │
│  [Đoạn 5] "Neural networks are..." (89%)  │
│  [Đoạn 7] "Backpropagation is..." (85%)   │
│                                            │
│  [View source] [Ask follow-up]             │
└────────────────────────────────────────────┘
```

---

## 🧪 Testing Checklist

- [ ] Chunk text correctly (no mid-sentence cuts)
- [ ] Generate embeddings via API
- [ ] Store and retrieve from vector DB
- [ ] Cosine similarity calculation correct
- [ ] Find top-K relevant chunks
- [ ] Build prompt with context
- [ ] Generate accurate answers
- [ ] Return source attributions
- [ ] Handle empty results gracefully
- [ ] Test with various document types (PDF, TXT)
- [ ] Test with Vietnamese and English

---

## 📚 Documentation Created

1. **RAG_SYSTEM_DESIGN.md** - Complete system architecture
2. **RAG_IMPLEMENTATION_GUIDE.md** - Step-by-step implementation
3. **RAG_SUMMARY.md** - This executive summary

---

## 🚀 Recommended Implementation Order

### Week 1: Core RAG
1. **Day 1-2:** EmbeddingService (OpenAI API integration)
2. **Day 3:** VectorStoreService (in-memory with cosine similarity)
3. **Day 4-5:** DocumentIndexingService (async pipeline)

### Week 2: Integration & Polish
4. **Day 1-2:** Enhanced RAGService (retrieval + prompt building)
5. **Day 3:** API endpoints (index, status, Q&A)
6. **Day 4-5:** Testing, debugging, optimization

---

## 💡 Key Design Decisions

### 1. Chunking Strategy: Fixed Size ✅
**Why:** Most reliable for RAG, prevents context loss with overlap

### 2. Vector Store: In-Memory MVP → Pinecone Later ✅
**Why:** Simple for demo, can scale later without code changes

### 3. Embeddings: OpenAI text-embedding-3-small ✅
**Why:** Cheap ($0.02/1M tokens), good quality, 1536 dims

### 4. Storage: JSON in NVARCHAR(MAX) ✅
**Why:** Compatible with SQL Server 2019+, flexible, no external deps

### 5. Async Indexing ✅
**Why:** Don't block upload, better UX

---

## ⚠️ Limitations & Future Improvements

### Current Limitations
- ⚠️ In-memory vector search (slow for 1000+ docs)
- ⚠️ No hybrid search (vector + keyword)
- ⚠️ No re-ranking
- ⚠️ Single document context only

### Future Improvements
- 🚀 External vector DB (Pinecone, Qdrant, Weaviate)
- 🚀 Hybrid search (BM25 + vector)
- 🚀 Cross-document search
- 🚀 Query rewriting
- 🚀 Answer validation
- 🚀 Citation extraction

---

## 📞 Questions & Answers

**Q: Tại sao phải chunk?**  
A: AI có token limit (~4K-8K context). Chunk giúp chỉ gửi phần liên quan, tiết kiệm chi phí.

**Q: Cosine similarity là gì?**  
A: Đo độ tương đồng giữa 2 vectors. Score 1.0 = giống hệt, 0.0 = không liên quan.

**Q: Tại sao dùng overlap?**  
A: Đảm bảo context không bị mất khi chia chunk. Ví dụ: câu cuối chunk 1 + câu đầu chunk 2.

**Q: OpenAI hay Gemini?**  
A: OpenAI embedding tốt hơn một chút, nhưng Gemini rẻ hơn. Cả 2 đều OK.

**Q: Có cần vector database chuyên dụng?**  
A: Không cần cho MVP (<1000 docs). Nhưng production nên dùng Pinecone/Qdrant.

---

## ✅ Ready to Start

**Phase 1 (Chunking):** ✅ DONE  
**Phase 2-4:** 📋 Documented, ready to implement  
**Estimated time:** 1-2 weeks for MVP  
**Cost:** ~$60/month for 1000 users  

All design decisions made, architecture solid, ready to code! 🚀

**Next:** Start implementing EmbeddingService 👉 See `RAG_IMPLEMENTATION_GUIDE.md`
