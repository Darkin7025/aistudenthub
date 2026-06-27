package com.example.swp391.aistudenthub.feature.chat.repository;

import com.example.swp391.aistudenthub.feature.chat.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository cho DocumentChunk entity.
 * Quản lý các chunks và embeddings của documents.
 */
@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, UUID> {
    
    /**
     * Tìm tất cả chunks của một document.
     *
     * @param documentId ID của document
     * @return List chunks được sắp xếp theo chunk_index
     */
    List<DocumentChunk> findByDocumentIdOrderByChunkIndexAsc(UUID documentId);
    
    /**
     * Đếm số chunks của một document.
     *
     * @param documentId ID của document
     * @return Số chunks
     */
    long countByDocumentId(UUID documentId);
    
    /**
     * Xóa tất cả chunks của một document.
     *
     * @param documentId ID của document
     */
    @Modifying
    @Query("DELETE FROM DocumentChunk dc WHERE dc.documentId = :documentId")
    void deleteByDocumentId(@Param("documentId") UUID documentId);
    
    /**
     * Kiểm tra xem document đã có chunks chưa.
     *
     * @param documentId ID của document
     * @return true nếu đã có chunks
     */
    boolean existsByDocumentId(UUID documentId);
}
