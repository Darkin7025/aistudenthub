package com.example.swp391.aistudenthub.feature.document.service;

import com.example.swp391.aistudenthub.config.OnlyOfficeConfig;
import com.example.swp391.aistudenthub.feature.auth.entity.Role;
import com.example.swp391.aistudenthub.feature.auth.entity.User;
import com.example.swp391.aistudenthub.feature.document.dto.request.OnlyOfficeCallbackRequest;
import com.example.swp391.aistudenthub.feature.document.dto.response.OnlyOfficeConfigResponse;
import com.example.swp391.aistudenthub.feature.document.entity.Document;
import com.example.swp391.aistudenthub.feature.document.enums.DocumentVisibility;
import com.example.swp391.aistudenthub.feature.document.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OnlyOfficeIntegrationTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private OnlyOfficeConfig onlyOfficeConfig;

    @InjectMocks
    private DocumentService documentService;

    private UUID docId;
    private UUID ownerId;
    private User owner;
    private Document document;

    @BeforeEach
    void setUp() {
        docId = UUID.randomUUID();
        ownerId = UUID.randomUUID();

        owner = User.builder()
                .id(ownerId)
                .email("owner@test.com")
                .role(Role.USER)
                .build();

        document = Document.builder()
                .id(docId)
                .userId(ownerId)
                .title("Lecture Note.docx")
                .originalFileName("Lecture Note.docx")
                .fileType("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                .fileUrl("https://cloudinary.com/doc.docx")
                .visibility(DocumentVisibility.PRIVATE)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        ReflectionTestUtils.setField(documentService, "appBaseUrl", "http://localhost:8080");
    }

    @Test
    void getOnlyOfficeConfig_Success_WhenOwner() {
        when(documentRepository.findByIdAndDeletedAtIsNull(docId)).thenReturn(Optional.of(document));
        when(onlyOfficeConfig.getDocserviceUrl()).thenReturn("http://localhost:8000/");
        when(onlyOfficeConfig.createToken(any())).thenReturn("mock-jwt-token");

        OnlyOfficeConfigResponse config = documentService.getOnlyOfficeConfig(docId, owner);

        assertNotNull(config);
        assertEquals("http://localhost:8000/web-apps/apps/api/documents/api.js", config.getDocserviceUrl());
        assertEquals("mock-jwt-token", config.getToken());
        assertEquals("word", config.getDocumentType());
        assertEquals("docx", config.getDocument().getFileType());
        assertTrue(config.getDocument().getPermissions().isEdit());
        assertEquals("edit", config.getEditorConfig().getMode());
        assertEquals("http://localhost:8080/api/v1/documents/" + docId + "/onlyoffice-callback",
                config.getEditorConfig().getCallbackUrl());
    }

    @Test
    void handleOnlyOfficeCallback_IgnoresStatus1() {
        OnlyOfficeCallbackRequest callback = OnlyOfficeCallbackRequest.builder()
                .status(1)
                .key("key123")
                .build();

        Map<String, Object> result = documentService.handleOnlyOfficeCallback(docId, callback);

        assertNotNull(result);
        assertEquals(0, result.get("error"));
        verify(documentRepository, never()).findByIdAndDeletedAtIsNull(any());
    }
}
