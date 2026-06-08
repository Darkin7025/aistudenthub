# 🔧 Fix Lỗi 401 - PDF Preview Cloudinary

## 📋 Tóm Tắt Vấn Đề

**Lỗi:** Khi preview PDF, browser nhận lỗi 401 Unauthorized từ Cloudinary URL.

**Nguyên nhân:** 
- Cloudinary upload PDF với `resource_type: "auto"` → phát hiện là "raw" type
- Raw files theo mặc định **không thể truy cập trực tiếp** qua URL từ browser (bảo mật)

**Giải pháp:**
- Upload PDF với `resource_type: "image"` thay vì "auto"
- Cloudinary hỗ trợ PDF như image type → có thể preview trực tiếp
- Thêm `access_mode: "public"` để đảm bảo file công khai

---

## 🛠️ Files Đã Sửa

### 1. **CloudinaryService.java** ✅
**Thay đổi:**
- Detect file PDF trước khi upload (qua contentType hoặc extension)
- Dùng `resource_type: "image"` cho PDF
- Dùng `resource_type: "auto"` cho file khác
- Thêm `access_mode: "public"`
- Return thêm `resource_type` từ Cloudinary response

**Code mới:**
```java
// PDF upload như "image" để có thể preview trực tiếp trong browser
if ("application/pdf".equals(contentType) || 
    (fileName != null && fileName.toLowerCase().endsWith(".pdf"))) {
    resourceType = "image";
    log.info("Uploading PDF as image type for direct preview: {}", fileName);
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
```

### 2. **Document.java** (Entity) ✅
**Thay đổi:**
- Thêm field mới: `storageResourceType`

```java
@Column(name = "storage_resource_type", length = 50)
private String storageResourceType;
```

### 3. **DocumentService.java** ✅
**Thay đổi:**
- Lưu `resource_type` từ Cloudinary vào DB

```java
.storageResourceType(uploadResult.get("resource_type"))
```

### 4. **Database Migration** ✅
**File:** `add-storage-resource-type.sql`
- Thêm cột `storage_resource_type` vào bảng `documents`
- Update PDF cũ sang `resource_type = 'image'`
- Update file khác sang `resource_type = 'auto'`

---

## 🚀 Hướng Dẫn Deploy

### **Bước 1: Run Migration SQL**
```bash
# Kết nối SQL Server
sqlcmd -S localhost,1444 -U sa -P YourPassword -i add-storage-resource-type.sql

# Hoặc dùng SQL Server Management Studio (SSMS)
# Copy nội dung file add-storage-resource-type.sql và execute
```

**Kiểm tra:**
```sql
-- Xem cấu trúc bảng
SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH, IS_NULLABLE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'documents' AND COLUMN_NAME = 'storage_resource_type';

-- Xem phân bố resource_type
SELECT storage_resource_type, file_type, COUNT(*) as count
FROM documents
WHERE deleted_at IS NULL
GROUP BY storage_resource_type, file_type;
```

### **Bước 2: Build Backend**
```bash
cd d:\SUPPORT_PROJECT\Phuc_SWP\aistudenthub
mvnw clean package -DskipTests
```

### **Bước 3: Restart Backend**
```bash
# Stop backend hiện tại (Ctrl+C)
# Start lại
mvnw spring-boot:run
```

### **Bước 4: Restart Frontend**
```bash
cd d:\SUPPORT_PROJECT\Phuc_SWP\aistudenthub_FE
npm run dev
```

---

## ✅ Test Cases

### **Test 1: Upload PDF Mới**

1. Login vào hệ thống: http://localhost:5173
2. Upload file PDF mới (ví dụ: `test.pdf`)
3. Kiểm tra database:

```sql
SELECT TOP 1
    id,
    file_name,
    file_url,
    storage_resource_type,
    storage_public_id
FROM documents
WHERE file_name LIKE '%.pdf'
ORDER BY created_at DESC;
```

**Expected:**
- `storage_resource_type` = `'image'`
- `file_url` có dạng: `https://res.cloudinary.com/.../image/upload/.../documents/test.pdf`

4. Copy `file_url`, mở tab mới, paste URL
**Expected:** PDF hiển thị trực tiếp trong browser (KHÔNG bị 401)

---

### **Test 2: Preview PDF Trong Modal**

1. Vào danh sách documents
2. Click nút **Preview** trên file PDF vừa upload
3. Kiểm tra Developer Console (F12)

**Expected:**
- ✅ KHÔNG có lỗi 401 Unauthorized
- ✅ Iframe hiển thị PDF từ Cloudinary
- ✅ Message: "Nếu PDF không hiển thị, vui lòng tải xuống."
- ✅ Có tab mở rộng "Xem nội dung văn bản đã trích xuất" (nếu có extracted text)

---

### **Test 3: Preview API Response**

```bash
# Lấy access token sau khi login
TOKEN="your_jwt_token_here"
DOCUMENT_ID="your_document_id_here"

curl -X GET "http://localhost:8080/api/documents/${DOCUMENT_ID}/preview" \
  -H "Authorization: Bearer ${TOKEN}"
```

**Expected Response:**
```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "documentId": "...",
    "fileName": "test.pdf",
    "previewUrl": "https://res.cloudinary.com/.../image/upload/.../test.pdf",
    "fileType": "application/pdf",
    "previewSupported": true,
    "previewMode": "PDF",
    "textContent": "Extracted text content...",
    "truncated": false,
    "aiSupported": true,
    "message": "Nếu PDF không hiển thị, vui lòng tải xuống."
  }
}
```

**Key checks:**
- ✅ `previewMode` = `"PDF"`
- ✅ `previewSupported` = `true`
- ✅ `previewUrl` bắt đầu bằng `https://res.cloudinary.com/`
- ✅ URL chứa `/image/upload/` (KHÔNG phải `/raw/upload/`)

---

### **Test 4: Upload File Khác (TXT, DOCX, Image)**

Upload các loại file khác để đảm bảo không ảnh hưởng:

**TXT file:**
- Expected: `previewMode = TEXT`, hiển thị nội dung văn bản

**Image (JPG, PNG):**
- Expected: `previewMode = IMAGE`, hiển thị ảnh

**DOCX:**
- Expected: `previewMode = OFFICE`, hiển thị qua Microsoft Office viewer

---

### **Test 5: PDF Cũ (Uploaded Trước Migration)**

1. Tìm PDF đã upload trước khi fix:
```sql
SELECT id, file_name, file_url, storage_resource_type
FROM documents
WHERE file_name LIKE '%.pdf'
  AND created_at < GETDATE() -- PDF cũ
ORDER BY created_at DESC;
```

2. Nếu `storage_resource_type` = `'image'` (đã được migration update):
   - Preview sẽ **VẪN BỊ LỖI 401** vì file thực tế trên Cloudinary vẫn là "raw" type
   
3. **Giải pháp cho PDF cũ:** Phải **upload lại** hoặc dùng Cloudinary Admin API để chuyển resource type

---

## ⚠️ Lưu Ý Quan Trọng

### **1. PDF Cũ Sẽ Vẫn Bị 401**
- Migration chỉ update **metadata trong DB**
- File thực tế trên Cloudinary vẫn là "raw" type
- **Cách fix:** Upload lại PDF hoặc dùng Cloudinary API chuyển đổi

### **2. Test Với PDF Mới**
- Fix này chỉ áp dụng cho **PDF upload SAU KHI deploy code mới**
- PDF cũ cần xử lý riêng (xem mục dưới)

### **3. Cloudinary Limits**
- Free tier: 25 GB storage, 25 GB bandwidth/month
- Nếu vượt giới hạn, URL sẽ trả về 401/403

---

## 🔄 Migration PDF Cũ (Optional)

Nếu muốn fix PDF đã upload trước đây:

### **Option 1: Re-upload Thủ Công**
- User tải xuống PDF cũ
- Xóa document cũ
- Upload lại

### **Option 2: Cloudinary Admin API (Advanced)**

```java
// Service method để migrate PDF cũ
public void migratePdfToImageType(UUID documentId) {
    Document doc = documentRepository.findById(documentId)
        .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND));
    
    if (!"application/pdf".equals(doc.getFileType())) {
        throw new AppException(ErrorCode.INVALID_OPERATION);
    }
    
    try {
        String oldPublicId = doc.getStoragePublicId();
        
        // Rename trong Cloudinary (chuyển từ raw → image)
        Map<?, ?> result = cloudinary.uploader().rename(
            oldPublicId,
            oldPublicId, // keep same name
            ObjectUtils.asMap(
                "resource_type", "raw",
                "to_type", "image",
                "overwrite", true
            )
        );
        
        String newUrl = (String) result.get("secure_url");
        doc.setFileUrl(newUrl);
        doc.setStorageResourceType("image");
        documentRepository.save(doc);
        
        log.info("Migrated PDF {} to image type", documentId);
    } catch (IOException e) {
        log.error("Failed to migrate PDF {}", documentId, e);
        throw new AppException(ErrorCode.UPLOAD_FAILED);
    }
}
```

**⚠️ Cảnh báo:** Cloudinary `rename` API có thể không hỗ trợ chuyển type. Best practice: upload lại.

---

## 🐛 Troubleshooting

### **Vẫn bị 401 sau khi deploy:**

1. **Check Backend Log:**
```bash
# Tìm dòng log khi upload
grep "Uploading PDF as image type" application.log
grep "Cloudinary upload success" application.log
```

2. **Check Cloudinary URL:**
- URL phải chứa `/image/upload/` (KHÔNG phải `/raw/upload/`)
- Ví dụ đúng: `https://res.cloudinary.com/your-cloud/image/upload/v123/documents/test.pdf`
- Ví dụ sai: `https://res.cloudinary.com/your-cloud/raw/upload/v123/documents/test.pdf`

3. **Check Database:**
```sql
SELECT file_name, file_url, storage_resource_type
FROM documents
WHERE file_name LIKE '%.pdf'
ORDER BY created_at DESC;
```

4. **Check Cloudinary Dashboard:**
- Login: https://cloudinary.com/console
- Vào Media Library → tìm file PDF
- Check "Resource Type" phải là "image"

---

## 📊 Kết Quả Mong Đợi

### **Trước Fix:**
```
User upload PDF → Cloudinary (raw type) → fileUrl có dạng /raw/upload/...
→ Browser access fileUrl → 401 Unauthorized ❌
```

### **Sau Fix:**
```
User upload PDF → Cloudinary (image type) → fileUrl có dạng /image/upload/...
→ Browser access fileUrl → PDF hiển thị trực tiếp ✅
```

---

## 📚 Tài Liệu Tham Khảo

- [Cloudinary PDF Documentation](https://cloudinary.com/documentation/image_transformations#pdf_files)
- [Cloudinary Upload Parameters](https://cloudinary.com/documentation/upload_parameters)
- [Cloudinary Resource Types](https://cloudinary.com/documentation/upload_images#resource_type)

---

## 🎯 Summary

| File | Thay Đổi | Lý Do |
|------|----------|-------|
| **CloudinaryService.java** | Upload PDF với `resource_type: "image"` | Cho phép preview trực tiếp trong browser |
| **Document.java** | Thêm field `storageResourceType` | Lưu resource type từ Cloudinary |
| **DocumentService.java** | Lưu resource_type vào DB | Track resource type cho delete/management |
| **add-storage-resource-type.sql** | Migration thêm cột | Schema update |

**Test ngay:** Upload PDF mới → Click Preview → PDF hiển thị trong iframe ✅
