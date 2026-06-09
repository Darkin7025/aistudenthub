package com.example.swp391.aistudenthub.feature.document.repository;

import com.example.swp391.aistudenthub.feature.document.entity.Folder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FolderRepository extends JpaRepository<Folder, UUID> {

    List<Folder> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID userId);

    Optional<Folder> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByUserIdAndNameAndDeletedAtIsNull(UUID userId, String name);
}
