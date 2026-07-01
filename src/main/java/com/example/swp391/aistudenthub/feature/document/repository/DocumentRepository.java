package com.example.swp391.aistudenthub.feature.document.repository;

import com.example.swp391.aistudenthub.feature.document.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID userId);
    Optional<Document> findByIdAndDeletedAtIsNull(UUID id);

    List<Document> findByUserIdAndFolderIdAndDeletedAtIsNull(UUID userId, UUID folderId);

    @org.springframework.data.jpa.repository.Query("SELECT d FROM Document d WHERE d.deletedAt IS NULL AND " +
            "d.userId = :userId AND " +
            "(:visibility IS NULL OR d.visibility = :visibility) AND " +
            "(:keyword IS NULL OR LOWER(d.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(d.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:subject IS NULL OR d.subject = :subject) AND " +
            "(:major IS NULL OR d.major = :major) AND " +
            "(:folderId IS NULL OR d.folderId = :folderId)")
    org.springframework.data.domain.Page<Document> searchAndFilter(
            @org.springframework.data.repository.query.Param("userId") UUID userId,
            @org.springframework.data.repository.query.Param("visibility") com.example.swp391.aistudenthub.feature.document.enums.DocumentVisibility visibility,
            @org.springframework.data.repository.query.Param("keyword") String keyword,
            @org.springframework.data.repository.query.Param("subject") String subject,
            @org.springframework.data.repository.query.Param("major") String major,
            @org.springframework.data.repository.query.Param("folderId") UUID folderId,
            org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT d.subject FROM Document d WHERE d.userId = :userId AND d.deletedAt IS NULL AND d.subject IS NOT NULL AND d.subject != ''")
    java.util.List<String> findDistinctSubjectsByUserId(@org.springframework.data.repository.query.Param("userId") UUID userId);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT d.major FROM Document d WHERE d.userId = :userId AND d.deletedAt IS NULL AND d.major IS NOT NULL AND d.major != ''")
    java.util.List<String> findDistinctMajorsByUserId(@org.springframework.data.repository.query.Param("userId") UUID userId);

    @org.springframework.data.jpa.repository.Query("SELECT d FROM Document d WHERE d.deletedAt IS NULL " +
            "AND d.visibility = com.example.swp391.aistudenthub.feature.document.enums.DocumentVisibility.PUBLIC AND " +
            "(:keyword IS NULL OR LOWER(d.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(d.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:subject IS NULL OR d.subject = :subject) AND " +
            "(:major IS NULL OR d.major = :major)")
    org.springframework.data.domain.Page<Document> searchPublicDocuments(
            @org.springframework.data.repository.query.Param("keyword") String keyword,
            @org.springframework.data.repository.query.Param("subject") String subject,
            @org.springframework.data.repository.query.Param("major") String major,
            org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT d.subject FROM Document d WHERE d.deletedAt IS NULL AND d.visibility = com.example.swp391.aistudenthub.feature.document.enums.DocumentVisibility.PUBLIC AND d.subject IS NOT NULL AND d.subject != ''")
    java.util.List<String> findDistinctPublicSubjects();

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT d.major FROM Document d WHERE d.deletedAt IS NULL AND d.visibility = com.example.swp391.aistudenthub.feature.document.enums.DocumentVisibility.PUBLIC AND d.major IS NOT NULL AND d.major != ''")
    java.util.List<String> findDistinctPublicMajors();

}
