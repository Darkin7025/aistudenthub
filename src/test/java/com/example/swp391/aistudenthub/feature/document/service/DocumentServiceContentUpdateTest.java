package com.example.swp391.aistudenthub.feature.document.service;

import com.example.swp391.aistudenthub.exception.AppException;
import com.example.swp391.aistudenthub.exception.ErrorCode;
import com.example.swp391.aistudenthub.feature.document.dto.request.DocumentContentUpdateRequest;
import com.example.swp391.aistudenthub.feature.document.dto.response.DocumentResponse;
import com.example.swp391.aistudenthub.feature.document.entity.Document;
import com.example.swp391.aistudenthub.feature.document.enums.DocumentVisibility;
import com.example.swp391.aistudenthub.feature.document.mapper.DocumentMapper;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceContentUpdateTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentMapper documentMapper;

    @InjectMocks
    private DocumentService documentService;

    private UUID docId;
    private UUID ownerId;
    private UUID strangerId;
    private Document document;

    @BeforeEach
    void setUp() {
        docId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
        strangerId = UUID.randomUUID();

        document = Document.builder()
                .id(docId)
                .userId(ownerId)
                .title("Sample Doc")
                .extractedText("Old text content")
                .visibility(DocumentVisibility.PRIVATE)
                .build();
    }

    @Test
    void updateDocumentContent_Success_WhenOwner() {
        when(documentRepository.findByIdAndDeletedAtIsNull(docId)).thenReturn(Optional.of(document));
        when(documentRepository.save(any(Document.class))).thenAnswer(i -> i.getArgument(0));

        DocumentResponse expectedResponse = DocumentResponse.builder()
                .id(docId)
                .extractedText("New edited text content")
                .build();
        when(documentMapper.toResponse(any(Document.class))).thenReturn(expectedResponse);

        DocumentContentUpdateRequest request = DocumentContentUpdateRequest.builder()
                .content("New edited text content")
                .build();

        DocumentResponse response = documentService.updateDocumentContent(docId, request, ownerId);

        assertNotNull(response);
        assertEquals("New edited text content", document.getExtractedText());
        verify(documentRepository, times(1)).save(document);
    }

    @Test
    void updateDocumentContent_ThrowsForbidden_WhenNotOwner() {
        when(documentRepository.findByIdAndDeletedAtIsNull(docId)).thenReturn(Optional.of(document));

        DocumentContentUpdateRequest request = DocumentContentUpdateRequest.builder()
                .content("Malicious edited content")
                .build();

        AppException exception = assertThrows(AppException.class, () ->
                documentService.updateDocumentContent(docId, request, strangerId));

        assertEquals(ErrorCode.FORBIDDEN_ACCESS, exception.getErrorCode());
        verify(documentRepository, never()).save(any());
    }
}
