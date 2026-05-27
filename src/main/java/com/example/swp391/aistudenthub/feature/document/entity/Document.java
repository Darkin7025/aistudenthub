package com.example.swp391.aistudenthub.feature.document.entity;

import jakarta.persistence.*;
import lombok.*;

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
    @Column(columnDefinition = "UNIQUEIDENTIFIER")
    private UUID id;

   
    @Column(name = "uploaded_by", nullable = false, columnDefinition = "UNIQUEIDENTIFIER")
    private UUID userId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 1000)
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

    @Column(name = "is_public", nullable = false)
    @Builder.Default
    private boolean isPublic = true;

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
