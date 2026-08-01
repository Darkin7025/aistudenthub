package com.example.swp391.aistudenthub.feature.document.repository;

import com.example.swp391.aistudenthub.feature.document.entity.DocumentShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentShareRepository extends JpaRepository<DocumentShare, UUID> {
    List<DocumentShare> findBySharedWithUserIdOrderByCreatedAtDesc(UUID sharedWithUserId);
    List<DocumentShare> findByDocumentId(UUID documentId);
    boolean existsByDocumentIdAndSharedWithUserId(UUID documentId, UUID sharedWithUserId);
    Optional<DocumentShare> findByDocumentIdAndSharedWithUserId(UUID documentId, UUID sharedWithUserId);
}
