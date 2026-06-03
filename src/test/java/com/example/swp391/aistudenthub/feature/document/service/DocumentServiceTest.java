package com.example.swp391.aistudenthub.feature.document.service;

import com.example.swp391.aistudenthub.exception.AppException;
import com.example.swp391.aistudenthub.exception.ErrorCode;
import com.example.swp391.aistudenthub.feature.document.entity.Document;
import com.example.swp391.aistudenthub.feature.document.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private CloudinaryService cloudinaryService;

    @InjectMocks
    private DocumentService documentService;

    private UUID documentId;
    private UUID userId;
    private Document document;

    @BeforeEach
    void setUp() {
        documentId = UUID.randomUUID();
        userId = UUID.randomUUID();
        document = Document.builder()
                .id(documentId)
                .userId(userId)
                .title("Test Doc")
                .isPublic(true)
                .build();
    }

    @Test
    void delete_Success() {
        // Given
        when(documentRepository.findByIdAndDeletedAtIsNull(documentId))
                .thenReturn(Optional.of(document));

        // When
        documentService.delete(documentId, userId);

        // Then
        assertNotNull(document.getDeletedAt());
        verify(documentRepository, times(1)).save(document);
    }

    @Test
    void delete_DocumentNotFound() {
        // Given
        when(documentRepository.findByIdAndDeletedAtIsNull(documentId))
                .thenReturn(Optional.empty());

        // When & Then
        AppException exception = assertThrows(AppException.class, () -> {
            documentService.delete(documentId, userId);
        });

        assertEquals(ErrorCode.DOCUMENT_NOT_FOUND, exception.getErrorCode());
        verify(documentRepository, never()).save(any());
    }

    @Test
    void delete_ForbiddenAccess() {
        // Given
        UUID differentUserId = UUID.randomUUID();
        when(documentRepository.findByIdAndDeletedAtIsNull(documentId))
                .thenReturn(Optional.of(document));

        // When & Then
        AppException exception = assertThrows(AppException.class, () -> {
            documentService.delete(documentId, differentUserId);
        });

        assertEquals(ErrorCode.FORBIDDEN_ACCESS, exception.getErrorCode());
        verify(documentRepository, never()).save(any());
    }
}
