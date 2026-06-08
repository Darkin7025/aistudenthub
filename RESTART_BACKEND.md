# 🔄 Restart Backend để Fix PDF Preview

## ⚠️ Vấn Đề Hiện Tại

Backend Java đang chạy từ **6:45:46 AM** - có thể là **TRƯỚC KHI CODE ĐƯỢC SỬA**.

Code hiện tại đã có fix:
- ✅ CloudinaryService upload PDF với `resource_type: "image"`
- ✅ Document entity có field `storageResourceType`
- ✅ DocumentService save resource_type vào DB

**NHƯNG** backend đang chạy vẫn là version cũ → PDF vẫn upload dưới dạng "raw" → Lỗi 401/không xem được!

## 🚀 Cách Restart Backend

### **Option 1: Nếu Backend đang chạy trong Terminal/IntelliJ**

1. **Tìm terminal đang chạy backend**
2. **Nhấn `Ctrl+C`** để stop
3. **Chạy lại:**
   ```bash
   cd d:\SUPPORT_PROJECT\Phuc_SWP\aistudenthub
   mvnw spring-boot:run
   ```

### **Option 2: Nếu Backend chạy trong IntelliJ IDEA**

1. Mở IntelliJ
2. Tìm tab "Run" ở dưới
3. Click nút "Stop" (hình vuông đỏ)
4. Click nút "Run" (hình tam giác xanh) hoặc `Shift+F10`

### **Option 3: Kill Process và Start Lại**

```bash
# 1. Kill tất cả Java processes
taskkill /F /IM java.exe

# 2. Đợi 5 giây
timeout /t 5

# 3. Start backend
cd d:\SUPPORT_PROJECT\Phuc_SWP\aistudenthub
mvnw spring-boot:run
```

## ✅ Verify Backend Đã Restart

### **Check 1: Log Output**

Khi backend start, bạn sẽ thấy log:
```
...
Started AistudenthubApplication in X.XXX seconds
Tomcat started on port(s): 8080 (http)
```

### **Check 2: Test Health Endpoint**

```bash
curl http://localhost:8080/actuator/health
```

Hoặc mở browser: http://localhost:8080/swagger-ui.html

### **Check 3: Upload PDF Mới**

1. Vào http://localhost:5173/documents
2. Upload PDF MỚI
3. Xem backend log, phải có dòng:
   ```
   Uploading PDF as image type for direct preview: test.pdf
   Cloudinary upload success: publicId=documents/..., resourceType=image
   ```

## 🧪 Test Sau Khi Restart

### **Bước 1: Upload PDF Mới**

1. Vào http://localhost:5173/documents
2. Click "Tải lên" / "Upload"
3. Chọn file PDF bất kỳ (ví dụ: `test-preview.pdf`)
4. Điền thông tin và upload

### **Bước 2: Check Database**

```sql
-- Kết nối SQL Server
USE ai_study_hub;

-- Lấy PDF vừa upload
SELECT TOP 1
    id,
    title,
    file_name,
    file_url,
    storage_resource_type,
    created_at
FROM documents
WHERE file_name LIKE '%.pdf'
  AND deleted_at IS NULL
ORDER BY created_at DESC;
```

**Expected:**
- `file_url` chứa `/image/upload/` (KHÔNG phải `/raw/upload/`)
- `storage_resource_type` = `'image'`

### **Bước 3: Test URL Trực Tiếp**

1. Copy `file_url` từ database
2. Mở tab mới trong browser
3. Paste URL và Enter

**Expected:**
- ✅ PDF hiển thị trực tiếp
- ❌ KHÔNG bị lỗi 401 Unauthorized

### **Bước 4: Test Preview trong App**

1. Vào http://localhost:5173/documents
2. Tìm PDF vừa upload
3. Click nút "Xem trước"

**Expected:**
- ✅ Modal mở
- ✅ PDF hiển thị trong iframe
- ✅ KHÔNG có popup "Lỗi - Không tải được tài liệu PDF"

## 🔍 Debug Nếu Vẫn Lỗi

### **Vấn đề 1: Backend Không Start**

**Lỗi:** Port 8080 đã được sử dụng

**Giải pháp:**
```bash
# Tìm process đang dùng port 8080
netstat -ano | findstr :8080

# Kill process (thay <PID> bằng số PID từ lệnh trên)
taskkill /F /PID <PID>

# Start lại backend
mvnw spring-boot:run
```

### **Vấn đề 2: Database Connection Error**

**Lỗi:** Cannot connect to SQL Server

**Giải pháp:**
1. Check SQL Server đang chạy:
   ```bash
   # Services
   services.msc
   # Tìm "SQL Server (MSSQLSERVER)" → Start nếu stopped
   ```

2. Check `application-local.properties`:
   ```properties
   spring.datasource.url=jdbc:sqlserver://localhost:1444;databaseName=ai_study_hub;encrypt=false;trustServerCertificate=true
   spring.datasource.username=sa
   spring.datasource.password=YourPassword
   ```

### **Vấn đề 3: Migration Chưa Chạy**

**Lỗi:** Column 'storage_resource_type' is invalid

**Giải pháp:** Run migration SQL:
```bash
sqlcmd -S localhost,1444 -U sa -P YourPassword -i add-storage-resource-type.sql
```

Hoặc mở SSMS và run nội dung file `add-storage-resource-type.sql`

## 📋 Checklist

Trước khi test PDF preview:

- [ ] Backend đã restart (Java process start sau khi code được sửa)
- [ ] Backend log có dòng "Started AistudenthubApplication"
- [ ] Database có cột `storage_resource_type`
- [ ] Frontend đã restart và clear cache
- [ ] Upload PDF **MỚI** (không phải PDF cũ)
- [ ] Backend log có "Uploading PDF as image type"
- [ ] Database: `file_url` chứa `/image/upload/`
- [ ] Test URL trực tiếp → PDF hiển thị (không 401)

## 🎯 Expected Result

**Backend Log khi upload PDF:**
```
2026-06-07 08:00:00.123  INFO --- Uploading PDF as image type for direct preview: test.pdf
2026-06-07 08:00:02.456  INFO --- Cloudinary upload success: publicId=documents/test_abc123, resourceType=image
2026-06-07 08:00:02.789  INFO --- Document saved: id=550e8400-e29b-41d4-a716-446655440000, user=...
```

**Database:**
```
file_url: https://res.cloudinary.com/your-cloud/image/upload/v123456/documents/test.pdf
storage_resource_type: image
```

**Browser:**
- Modal preview → PDF hiển thị ngay trong iframe ✅
- No errors in Console (F12) ✅

---

**TL;DR:** 
1. Stop backend (Ctrl+C hoặc taskkill)
2. Start backend (`mvnw spring-boot:run`)
3. Upload PDF MỚI
4. Preview → Success! 🎉
