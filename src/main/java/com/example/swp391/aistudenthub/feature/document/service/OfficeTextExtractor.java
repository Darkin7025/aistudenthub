package com.example.swp391.aistudenthub.feature.document.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Extracts plain text from Microsoft Office Open XML files (.docx, .xlsx, .pptx)
 * using Apache POI. Only modern XML-based formats are supported
 * (NOT legacy .doc / .xls / .ppt).
 */
@Slf4j
@Component
public class OfficeTextExtractor {

    /**
     * Auto-detects file type by name/mime and delegates to the correct extractor.
     */
    public String extract(byte[] fileBytes, String fileName, String mimeType) {
        String lowerName = fileName != null ? fileName.toLowerCase() : "";
        String lowerMime = mimeType != null ? mimeType.toLowerCase() : "";

        if (lowerName.endsWith(".docx") ||
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(lowerMime)) {
            return extractDocx(fileBytes);
        }
        if (lowerName.endsWith(".xlsx") ||
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet".equals(lowerMime)) {
            return extractXlsx(fileBytes);
        }
        if (lowerName.endsWith(".pptx") ||
                "application/vnd.openxmlformats-officedocument.presentationml.presentation".equals(lowerMime)) {
            return extractPptx(fileBytes);
        }
        log.warn("No Office extractor matched for fileName={}, mimeType={}", fileName, mimeType);
        return null;
    }

    private String extractDocx(byte[] fileBytes) {
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(fileBytes));
             XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
            String text = extractor.getText();
            if (text != null && !text.trim().isEmpty()) {
                log.info("Extracted {} chars from .docx", text.length());
                return text;
            }
            log.warn(".docx file yielded no text");
            return null;
        } catch (IOException e) {
            log.error("Failed to extract text from .docx: {}", e.getMessage());
            return null;
        }
    }

    private String extractXlsx(byte[] fileBytes) {
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(fileBytes))) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                var sheet = wb.getSheetAt(i);
                sb.append("[Sheet: ").append(sheet.getSheetName()).append("]\n");
                for (var row : sheet) {
                    for (var cell : row) {
                        String val = cell.toString();
                        if (!val.isBlank()) {
                            sb.append(val).append("\t");
                        }
                    }
                    sb.append("\n");
                }
                sb.append("\n");
            }
            String text = sb.toString().trim();
            if (!text.isEmpty()) {
                log.info("Extracted {} chars from .xlsx", text.length());
                return text;
            }
            log.warn(".xlsx file yielded no text");
            return null;
        } catch (IOException e) {
            log.error("Failed to extract text from .xlsx: {}", e.getMessage());
            return null;
        }
    }

    private String extractPptx(byte[] fileBytes) {
        try (XMLSlideShow ppt = new XMLSlideShow(new ByteArrayInputStream(fileBytes))) {
            StringBuilder sb = new StringBuilder();
            for (XSLFSlide slide : ppt.getSlides()) {
                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape textShape) {
                        String text = textShape.getText();
                        if (text != null && !text.isBlank()) {
                            sb.append(text).append("\n");
                        }
                    }
                }
            }
            String text = sb.toString().trim();
            if (!text.isEmpty()) {
                log.info("Extracted {} chars from .pptx", text.length());
                return text;
            }
            log.warn(".pptx file yielded no text");
            return null;
        } catch (IOException e) {
            log.error("Failed to extract text from .pptx: {}", e.getMessage());
            return null;
        }
    }
}
