package com.example.swp391.aistudenthub.feature.document.entity;

import jakarta.persistence.*;
import lombok.*;

import com.example.swp391.aistudenthub.feature.document.enums.DocumentVisibility;
import com.example.swp391.aistudenthub.feature.document.enums.UploadStatus;

import java.time.OffsetDateTime;
import java.util.UUID;


@Entity
@Table(name = "documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

   
    @Column(name = "uploaded_by", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;


    @Column(name = "file_url", nullable = false, length = 2000)
    private String fileUrl;

  
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    
    @Column(name = "original_file_name", length = 255)
    private String originalFileName;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "file_type", length = 100)
    private String fileType;

    @Column(name = "storage_public_id", nullable = false, length = 500)
    private String storagePublicId;

    @Column(name = "storage_key", nullable = false, length = 500)
    private String storageKey;

    @Column(name = "storage_resource_type", length = 50)
    private String storageResourceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false)
    @Builder.Default
    private DocumentVisibility visibility = DocumentVisibility.PRIVATE;

    @Column(name = "subject", length = 255)
    private String subject;

    @Column(name = "major", length = 255)
    private String major;

    @Column(name = "document_type", length = 100)
    private String documentType;

    @Column(name = "folder_id")
    private UUID folderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "upload_status")
    @Builder.Default
    private UploadStatus uploadStatus = UploadStatus.COMPLETED;

    @Column(name = "upload_progress")
    @Builder.Default
    private Integer uploadProgress = 100;

    @Column(name = "extracted_text", columnDefinition = "TEXT")
    private String extractedText;

    @Column(name = "custom_metadata", columnDefinition = "TEXT")
    private String customMetadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @Column(name = "storage_bucket", nullable = false)
    private String storageBucket;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
