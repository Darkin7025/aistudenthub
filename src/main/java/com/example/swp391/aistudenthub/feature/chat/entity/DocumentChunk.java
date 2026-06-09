package com.example.swp391.aistudenthub.feature.chat.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entity lưu trữ các chunks của document cùng với embeddings.
 * Phục vụ cho RAG system - vector search.
 */
@Entity
@Table(name = "document_chunks", indexes = {
    @Index(name = "idx_document_chunks_doc_id", columnList = "document_id"),
    @Index(name = "idx_document_chunks_doc_chunk", columnList = "document_id, chunk_index")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentChunk {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    /**
     * ID của document gốc.
     */
    @Column(name = "document_id", nullable = false)
    private UUID documentId;
    
    /**
     * Index của chunk trong document (0-based).
     */
    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;
    
    /**
     * Nội dung text của chunk.
     */
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;
    
    /**
     * Embedding vector được lưu dạng JSON string.
     * Format: "[0.123, 0.456, ...]"
     * 
     * Note: SQL Server 2025+ hỗ trợ vector type native,
     * nhưng để tương thích ta dùng NVARCHAR(MAX) + JSON.
     */
    @Column(name = "embedding", columnDefinition = "TEXT")
    private String embedding;
    
    /**
     * Số tokens ước tính trong chunk.
     */
    @Column(name = "token_count")
    private Integer tokenCount;
    
    /**
     * Vị trí bắt đầu trong document gốc (word index).
     */
    @Column(name = "start_position")
    private Integer startPosition;
    
    /**
     * Vị trí kết thúc trong document gốc (word index).
     */
    @Column(name = "end_position")
    private Integer endPosition;
    
    /**
     * Thời điểm tạo chunk.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}
