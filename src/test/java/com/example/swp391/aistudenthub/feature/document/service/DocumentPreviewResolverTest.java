package com.example.swp391.aistudenthub.feature.document.service;

import com.example.swp391.aistudenthub.feature.document.enums.PreviewMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DocumentPreviewResolverTest {

    private final DocumentPreviewResolver resolver = new DocumentPreviewResolver();

    @Test
    void resolvesTextFilesByExtensionWhenMimeIsOctetStream() {
        assertEquals(PreviewMode.TEXT, resolver.resolveMode("Main.java", "application/octet-stream"));
        assertEquals(PreviewMode.TEXT, resolver.resolveMode(".env", "application/octet-stream"));
    }

    @Test
    void resolvesOfficeFilesByMimeOrExtension() {
        assertEquals(PreviewMode.OFFICE, resolver.resolveMode("report.bin",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        assertEquals(PreviewMode.OFFICE, resolver.resolveMode("slides.pptx", "application/octet-stream"));
    }

    @Test
    void resolvesCommonPreviewModesAndUnsupportedFiles() {
        assertEquals(PreviewMode.PDF, resolver.resolveMode("sample.pdf", "application/pdf"));
        assertEquals(PreviewMode.IMAGE, resolver.resolveMode("image.bmp", "application/octet-stream"));
        assertEquals(PreviewMode.UNSUPPORTED, resolver.resolveMode("source.zip", "application/zip"));
    }
}
