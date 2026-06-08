package com.example.swp391.aistudenthub.feature.document.service;

import com.example.swp391.aistudenthub.feature.document.enums.PreviewMode;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class DocumentPreviewResolver {

    private static final Set<String> TEXT_EXTENSIONS = Set.of(
            ".txt", ".md", ".csv", ".json", ".xml", ".yml", ".yaml", 
            ".sql", ".java", ".js", ".jsx", ".ts", ".tsx", ".py", 
            ".dart", ".html", ".css", ".scss", ".log", ".properties",
            ".env", ".gitignore"
    );

    private static final Set<String> OFFICE_EXTENSIONS = Set.of(
            ".doc", ".docx", ".ppt", ".pptx", ".xls", ".xlsx"
    );

    private static final Set<String> OFFICE_MIME_TYPES = Set.of(
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    private static final Set<String> UNSUPPORTED_EXTENSIONS = Set.of(
            ".zip", ".rar", ".7z", ".exe", ".jar", ".bin", ".apk", ".iso"
    );

    private static final Set<String> IMAGE_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp"
    );

    public PreviewMode resolveMode(String fileName, String mimeType) {
        String lowerName = fileName != null ? fileName.toLowerCase() : "";
        String lowerMime = mimeType != null ? mimeType.toLowerCase() : "";

        if (lowerName.endsWith(".pdf") || "application/pdf".equals(lowerMime)) {
            return PreviewMode.PDF;
        }

        if (lowerMime.startsWith("image/") || IMAGE_EXTENSIONS.stream().anyMatch(lowerName::endsWith)) {
            return PreviewMode.IMAGE;
        }

        if (lowerMime.startsWith("text/")
                || "application/json".equals(lowerMime)
                || "application/xml".equals(lowerMime)
                || TEXT_EXTENSIONS.stream().anyMatch(lowerName::endsWith)) {
            return PreviewMode.TEXT;
        }

        if (OFFICE_MIME_TYPES.contains(lowerMime) || OFFICE_EXTENSIONS.stream().anyMatch(lowerName::endsWith)) {
            return PreviewMode.OFFICE;
        }

        if (UNSUPPORTED_EXTENSIONS.stream().anyMatch(lowerName::endsWith)) {
            return PreviewMode.UNSUPPORTED;
        }

        return PreviewMode.UNSUPPORTED;
    }

    public boolean isAiCapable(PreviewMode previewMode) {
        return PreviewMode.TEXT.equals(previewMode) || PreviewMode.PDF.equals(previewMode);
    }
}
