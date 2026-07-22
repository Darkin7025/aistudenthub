package com.example.swp391.aistudenthub.feature.admin.service;

import com.example.swp391.aistudenthub.feature.admin.dto.response.AdminDocumentResponse;
import com.example.swp391.aistudenthub.feature.document.entity.Document;
import com.example.swp391.aistudenthub.feature.document.enums.DocumentVisibility;
import com.example.swp391.aistudenthub.feature.document.enums.UploadStatus;
import com.example.swp391.aistudenthub.feature.document.repository.DocumentRepository;
import com.example.swp391.aistudenthub.feature.document.service.DocumentPreviewResolver;
import com.example.swp391.aistudenthub.feature.document.service.DocumentService;
import com.example.swp391.aistudenthub.feature.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminDocumentServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private SystemLogService systemLogService;

    @Mock
    private DocumentService documentService;

    @Mock
    private DocumentPreviewResolver previewResolver;

    @InjectMocks
    private AdminServiceImpl adminService;

    private UUID docId;
    private UUID adminUserId;
    private Document document;

    @BeforeEach
    void setUp() {
        docId = UUID.randomUUID();
        adminUserId = UUID.randomUUID();

        document = Document.builder()
                .id(docId)
                .userId(adminUserId)
                .title("Original Title")
                .description("Original Description")
                .visibility(DocumentVisibility.PRIVATE)
                .uploadStatus(UploadStatus.PROCESSING)
                .createdAt(OffsetDateTime.now().minusDays(1))
                .build();
    }

    @Test
    void getAllDocuments_DefaultSortsByCreatedAtDesc() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Document> page = new PageImpl<>(List.of(document));

        when(documentRepository.searchAllDocumentsAdmin(
                eq(null), eq(null), eq(null), eq(null), eq(null), eq(null), eq(null), eq(false), any(Pageable.class)))
                .thenReturn(page);

        Page<AdminDocumentResponse> result = adminService.getAllDocuments(
                null, null, null, null, null, null, null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(documentRepository).searchAllDocumentsAdmin(
                eq(null), eq(null), eq(null), eq(null), eq(null), eq(null), eq(null), eq(false), pageableCaptor.capture());

        Pageable capturedPageable = pageableCaptor.getValue();
        assertTrue(capturedPageable.getSort().isSorted());
        assertNotNull(capturedPageable.getSort().getOrderFor("createdAt"));
        assertEquals(Sort.Direction.DESC, capturedPageable.getSort().getOrderFor("createdAt").getDirection());
    }
}
