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
            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "resource_type", "auto",   // tự detect: image / video / raw
                            "folder",        "documents" // tổ chức file theo folder
                    )
            );

            String url      = (String) result.get("secure_url");
            String publicId = (String) result.get("public_id");

            log.info("Cloudinary upload success: publicId={}", publicId);
            return Map.of("url", url, "public_id", publicId);

        } catch (IOException e) {
            log.error("Cloudinary upload failed", e);
            throw new AppException(ErrorCode.UPLOAD_FAILED);
        }
    }


    public void delete(String publicId, String resourceType) {
        try {
            cloudinary.uploader().destroy(
                    publicId,
                    ObjectUtils.asMap("resource_type", resourceType)
            );
            log.info("Cloudinary delete success: publicId={}", publicId);
        } catch (IOException e) {
            // Log nhưng không throw — file có thể đã bị xóa thủ công
            log.warn("Cloudinary delete failed for publicId={}: {}", publicId, e.getMessage());
        }
    }
}
