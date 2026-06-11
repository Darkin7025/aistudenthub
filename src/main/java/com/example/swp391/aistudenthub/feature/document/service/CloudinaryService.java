package com.example.swp391.aistudenthub.feature.document.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.swp391.aistudenthub.exception.AppException;
import com.example.swp391.aistudenthub.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    @SuppressWarnings("unchecked")
    public Map<String, String> upload(MultipartFile file) {
        java.io.File tempFile = null;
        try {
            tempFile = java.io.File.createTempFile("upload_", "_" + file.getOriginalFilename());
            file.transferTo(tempFile);

            String resourceType = "auto";
            String contentType = file.getContentType();
            String fileName = file.getOriginalFilename();

            if ("application/pdf".equals(contentType) ||
                (fileName != null && fileName.toLowerCase().endsWith(".pdf"))) {
                resourceType = "raw";
                log.info("Uploading PDF as raw type: {}", fileName);
            }

            log.info("Cloudinary upload start — file={}, size={}, contentType={}, resourceType={}",
                    fileName, file.getSize(), contentType, resourceType);

            Map<?, ?> result = cloudinary.uploader().upload(
                    tempFile,
                    ObjectUtils.asMap(
                            "resource_type", resourceType,
                            "folder", "documents",
                            "use_filename", true,
                            "unique_filename", true,
                            "access_mode", "public"
                    ));

            String url = (String) result.get("secure_url");
            String publicId = (String) result.get("public_id");
            String actualResourceType = (String) result.get("resource_type");

            log.info("Cloudinary upload success: publicId={}, resourceType={}, url={}",
                    publicId, actualResourceType, url);

            return Map.of(
                    "url", url,
                    "public_id", publicId,
                    "resource_type", actualResourceType != null ? actualResourceType : "auto"
            );

        } catch (IOException e) {
            log.error("Cloudinary upload failed (IOException) — file={}: {}",
                    file.getOriginalFilename(), e.getMessage(), e);
            throw new AppException(ErrorCode.UPLOAD_FAILED);
        } catch (Exception e) {
            // Catches Cloudinary ApiException, RuntimeException, auth errors, etc.
            log.error("Cloudinary upload failed ({}): {}",
                    e.getClass().getSimpleName(), e.getMessage(), e);
            throw new AppException(ErrorCode.UPLOAD_FAILED);
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    public void delete(String publicId, String resourceType) {
        try {
            cloudinary.uploader().destroy(
                    publicId,
                    ObjectUtils.asMap("resource_type", resourceType));
            log.info("Cloudinary delete success: publicId={}", publicId);
        } catch (IOException e) {
            log.warn("Cloudinary delete failed for publicId={}: {}", publicId, e.getMessage());
        }
    }

    public String getSignedUrl(String publicId, String resourceType, String format) {
        try {
            com.cloudinary.Url urlBuilder = cloudinary.url()
                    .resourceType(resourceType)
                    .signed(true);
            
            if (format != null && !format.isEmpty()) {
                urlBuilder.format(format);
            }
            
            return urlBuilder.generate(publicId);
        } catch (Exception e) {
            log.error("Failed to generate signed url for {}", publicId, e);
            return null;
        }
    }
}
