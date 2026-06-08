package com.example.swp391.aistudenthub.feature.document.service;

import com.example.swp391.aistudenthub.common.dto.MessageResponse;
import com.example.swp391.aistudenthub.exception.AppException;
import com.example.swp391.aistudenthub.exception.ErrorCode;
import com.example.swp391.aistudenthub.feature.document.dto.request.FolderRequest;
import com.example.swp391.aistudenthub.feature.document.dto.response.FolderResponse;
import com.example.swp391.aistudenthub.feature.document.entity.Document;
import com.example.swp391.aistudenthub.feature.document.entity.Folder;
import com.example.swp391.aistudenthub.feature.document.repository.DocumentRepository;
import com.example.swp391.aistudenthub.feature.document.repository.FolderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FolderService {

    private final FolderRepository folderRepository;
    private final DocumentRepository documentRepository;

    @Transactional(readOnly = true)
    public List<FolderResponse> getMyFolders(UUID userId) {
        return folderRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public FolderResponse createFolder(FolderRequest request, UUID userId) {
        String name = request.getName().trim();
        
        if (folderRepository.existsByUserIdAndNameAndDeletedAtIsNull(userId, name)) {
        }

        UUID parentId = request.getParentId();
        if (parentId != null) {
            Folder parent = folderRepository.findByIdAndDeletedAtIsNull(parentId)
                    .orElseThrow(() -> new AppException(ErrorCode.VALIDATION_ERROR));
            if (!parent.getUserId().equals(userId)) {
                throw new AppException(ErrorCode.FORBIDDEN_ACCESS);
            }
        }

        Folder folder = Folder.builder()
                .userId(userId)
                .name(name)
                .description(request.getDescription())
                .color(request.getColor())
                .parentId(parentId)
                .build();

        Folder saved = folderRepository.save(folder);
        return mapToResponse(saved);
    }

    @Transactional
    public FolderResponse updateFolder(UUID id, FolderRequest request, UUID userId) {
        Folder folder = folderRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ErrorCode.FOLDER_NOT_FOUND));

        if (!folder.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.FORBIDDEN_ACCESS);
        }

        String newName = request.getName().trim();
        folder.setName(newName);
        folder.setDescription(request.getDescription());
        folder.setColor(request.getColor());
        
        UUID parentId = request.getParentId();
        if (parentId != null && !parentId.equals(folder.getParentId())) {
            if (parentId.equals(folder.getId())) {
                throw new AppException(ErrorCode.VALIDATION_ERROR);
            }
            Folder parent = folderRepository.findByIdAndDeletedAtIsNull(parentId)
                    .orElseThrow(() -> new AppException(ErrorCode.VALIDATION_ERROR));
            if (!parent.getUserId().equals(userId)) {
                throw new AppException(ErrorCode.FORBIDDEN_ACCESS);
            }
        }
        folder.setParentId(parentId);

        Folder saved = folderRepository.save(folder);
        return mapToResponse(saved);
    }

    @Transactional
    public MessageResponse deleteFolder(UUID id, UUID userId) {
        Folder folder = folderRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ErrorCode.FOLDER_NOT_FOUND));

        if (!folder.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.FORBIDDEN_ACCESS);
        }

        List<Folder> allFolders = folderRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId);
        List<Folder> foldersToDelete = new java.util.ArrayList<>();
        foldersToDelete.add(folder);
        collectDescendantFolders(id, allFolders, foldersToDelete);
        
        java.time.OffsetDateTime now = java.time.OffsetDateTime.now();
        List<UUID> folderIdsToDelete = foldersToDelete.stream().map(Folder::getId).toList();

        for (Folder f : foldersToDelete) {
            f.setDeletedAt(now);
        }
        folderRepository.saveAll(foldersToDelete);

        List<Document> allDocuments = documentRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId);
        List<Document> documentsToDelete = allDocuments.stream()
                .filter(d -> folderIdsToDelete.contains(d.getFolderId()))
                .toList();
        
        for (Document d : documentsToDelete) {
            d.setDeletedAt(now);
        }
        if (!documentsToDelete.isEmpty()) {
            documentRepository.saveAll(documentsToDelete);
        }

        return new MessageResponse("Đã xóa thư mục và toàn bộ nội dung bên trong thành công");
    }

    private void collectDescendantFolders(UUID folderId, List<Folder> allFolders, List<Folder> descendants) {
        for (Folder f : allFolders) {
            if (folderId.equals(f.getParentId())) {
                descendants.add(f);
                collectDescendantFolders(f.getId(), allFolders, descendants);
            }
        }
    }

    private FolderResponse mapToResponse(Folder folder) {
        return FolderResponse.builder()
                .id(folder.getId())
                .name(folder.getName())
                .description(folder.getDescription())
                .color(folder.getColor())
                .parentId(folder.getParentId())
                .createdAt(folder.getCreatedAt())
                .updatedAt(folder.getUpdatedAt())
                .documentCount(0L)
                .build();
    }
}
