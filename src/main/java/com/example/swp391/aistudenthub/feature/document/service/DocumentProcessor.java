package com.example.swp391.aistudenthub.feature.document.service;

import com.example.swp391.aistudenthub.feature.document.entity.Document;
import com.example.swp391.aistudenthub.feature.document.enums.PreviewMode;
import com.example.swp391.aistudenthub.feature.document.enums.UploadStatus;
import com.example.swp391.aistudenthub.feature.document.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentProcessor {

    private final DocumentRepository documentRepository;
    private final DocumentPreviewResolver previewResolver;
    private final OfficeTextExtractor officeTextExtractor;
    private final com.example.swp391.aistudenthub.feature.chat.repository.DocumentChunkRepository documentChunkRepository;
    private final com.example.swp391.aistudenthub.feature.chat.service.ChunkingService chunkingService;

    @Async("documentTaskExecutor")
    public void processDocumentText(UUID documentId) {
        log.info("Starting background text extraction for document: {}", documentId);
        Document document = documentRepository.findById(documentId).orElse(null);
        if (document == null) {
            log.error("Document not found in background processor: {}", documentId);
            return;
        }

        try {
            String fileUrl = document.getFileUrl();
            String fileName = document.getFileName();
            String contentType = document.getFileType();

            PreviewMode previewMode = previewResolver.resolveMode(fileName, contentType);
            String extractedText = null;

            boolean shouldExtract = PreviewMode.TEXT.equals(previewMode)
                    || PreviewMode.PDF.equals(previewMode)
                    || (PreviewMode.OFFICE.equals(previewMode) && previewResolver.isOfficeAiCapable(fileName, contentType));

            if (shouldExtract) {
                log.info("Downloading file from url: {}", fileUrl);
                byte[] fileBytes = downloadFile(fileUrl);
                
                if (PreviewMode.TEXT.equals(previewMode)) {
                    extractedText = new String(fileBytes, StandardCharsets.UTF_8);
                } else if (PreviewMode.PDF.equals(previewMode)) {
                    try (PDDocument pdDoc = Loader.loadPDF(fileBytes)) {
                        PDFTextStripper stripper = new PDFTextStripper();
                        extractedText = stripper.getText(pdDoc);
                    }
                } else if (PreviewMode.OFFICE.equals(previewMode)) {
                    extractedText = officeTextExtractor.extract(fileBytes, fileName, contentType);
                }
            }

            if (extractedText != null) {
                String trimmed = extractedText.trim();
                if (trimmed.isEmpty()) {
                    extractedText = null;
                } else {
                    int maxLength = 500 * 1024; // 500KB / 512,000 characters limit
                    if (trimmed.length() > maxLength) {
                        extractedText = trimmed.substring(0, maxLength) + "\n\n[Nội dung đã bị cắt vì quá dài]";
                    } else {
                        extractedText = trimmed;
                    }
                }
            }

            documentRepository.updateExtractedTextAndStatus(documentId, extractedText, UploadStatus.COMPLETED, 100);
            
            // Perform chunking and save chunks to the database
            if (extractedText != null) {
                try {
                    documentChunkRepository.deleteByDocumentId(documentId);
                    java.util.List<com.example.swp391.aistudenthub.feature.chat.dto.TextChunk> textChunks = chunkingService.chunkText(extractedText);
                    java.util.List<com.example.swp391.aistudenthub.feature.chat.entity.DocumentChunk> docChunks = new java.util.ArrayList<>();
                    for (com.example.swp391.aistudenthub.feature.chat.dto.TextChunk tc : textChunks) {
                        docChunks.add(com.example.swp391.aistudenthub.feature.chat.entity.DocumentChunk.builder()
                                .documentId(documentId)
                                .chunkIndex(tc.getChunkIndex())
                                .content(tc.getContent())
                                .tokenCount(tc.getTokenCount())
                                .build());
                    }
                    if (!docChunks.isEmpty()) {
                        documentChunkRepository.saveAll(docChunks);
                        log.info("Saved {} chunks for document: {}", docChunks.size(), documentId);
                    }
                } catch (Exception e) {
                    log.error("Failed to chunk and save document chunks for document {}: {}", documentId, e.getMessage(), e);
                }
            }

            log.info("Background text extraction completed for document: {}, status: COMPLETED", documentId);

        } catch (Exception e) {
            log.error("Error during background text extraction for document {}: {}", documentId, e.getMessage(), e);
            documentRepository.updateUploadStatus(documentId, UploadStatus.FAILED);
        }
    }

    /** Rebuilds chunks after extraction or manual content edits. */
    public void reindexChunks(UUID documentId, String extractedText) {
        try {
            documentChunkRepository.deleteByDocumentId(documentId);
            if (extractedText == null || extractedText.isBlank()) {
                return;
            }

            java.util.List<com.example.swp391.aistudenthub.feature.chat.dto.TextChunk> textChunks =
                    chunkingService.chunkText(extractedText);
            java.util.List<com.example.swp391.aistudenthub.feature.chat.entity.DocumentChunk> docChunks = new java.util.ArrayList<>();
            for (com.example.swp391.aistudenthub.feature.chat.dto.TextChunk tc : textChunks) {
                docChunks.add(com.example.swp391.aistudenthub.feature.chat.entity.DocumentChunk.builder()
                        .documentId(documentId)
                        .chunkIndex(tc.getChunkIndex())
                        .content(tc.getContent())
                        .tokenCount(tc.getTokenCount())
                        .build());
            }
            if (!docChunks.isEmpty()) {
                documentChunkRepository.saveAll(docChunks);
                log.info("Saved {} chunks for document: {}", docChunks.size(), documentId);
            }
        } catch (Exception e) {
            log.error("Failed to rebuild document chunks for {}: {}", documentId, e.getMessage(), e);
            throw e;
        }
    }

    private byte[] downloadFile(String fileUrl) throws Exception {
        URL url = new URL(fileUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);
        try (InputStream in = conn.getInputStream()) {
            return in.readAllBytes();
        }
    }
}
