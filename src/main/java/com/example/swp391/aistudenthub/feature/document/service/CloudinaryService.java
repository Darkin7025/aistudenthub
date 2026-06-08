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

/**
 * Service xử lý tương tác với Cloudinary.
 * Chỉ chịu trách nhiệm upload / delete file — không biết về Document entity.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    @SuppressWarnings("unchecked")
    public Map<String, String> upload(MultipartFile file) {
        try {
            java.io.File tempFile = java.io.File.createTempFile("upload_", "_" + file.getOriginalFilename());
            file.transferTo(tempFile);

            // Xác định resource_type phù hợp
            String resourceType = "auto";
            String contentType = file.getContentType();
            String fileName = file.getOriginalFilename();
            
            // Upload PDF as "raw" to bypass Cloudinary's default PDF delivery restrictions
            if ("application/pdf".equals(contentType) || 
                (fileName != null && fileName.toLowerCase().endsWith(".pdf"))) {
                resourceType = "raw";
                log.info("Uploading PDF as raw type to bypass restrictions: {}", fileName);
            }

            Map<?, ?> result = cloudinary.uploader().upload(
                    tempFile,
                    ObjectUtils.asMap(
                            "resource_type", resourceType,
                            "folder", "documents",
                            "use_filename", true,
                            "unique_filename", true,
                            "access_mode", "public" // đảm bảo file có thể truy cập công khai
                    ));
            
            tempFile.delete();

            String url = (String) result.get("secure_url");
            String publicId = (String) result.get("public_id");
            String actualResourceType = (String) result.get("resource_type");

            log.info("Cloudinary upload success: publicId={}, resourceType={}", publicId, actualResourceType);
            return Map.of(
                "url", url, 
                "public_id", publicId,
                "resource_type", actualResourceType != null ? actualResourceType : "auto"
            );

        } catch (IOException e) {
            log.error("Cloudinary upload failed", e);
            throw new AppException(ErrorCode.UPLOAD_FAILED);
        }
    }

    public void delete(String publicId, String resourceType) {
        try {
            cloudinary.uploader().destroy(
                    publicId,
                    ObjectUtils.asMap("resource_type", resourceType));
            log.info("Cloudinary delete success: publicId={}", publicId);
        } catch (IOException e) {
            // Log nhưng không throw — file có thể đã bị xóa thủ công
            log.warn("Cloudinary delete failed for publicId={}: {}", publicId, e.getMessage());
        }
    }

    public String getSignedUrl(String publicId, String resourceType) {
        try {
            return cloudinary.url()
                    .resourceType(resourceType)
                    .signed(true)
                    .generate(publicId);
        } catch (Exception e) {
            log.error("Failed to generate signed url for {}", publicId, e);
            return null;
        }
    }
}
