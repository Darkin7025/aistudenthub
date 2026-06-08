# 🚀 Quick Fix - PDF Preview "localhost đã từ chối kết nối"

## ❌ Vấn Đề

Sau khi upload PDF/image, bấm "Xem trước" → Modal hiển thị nhưng:
- Iframe trống/không load
- Console lỗi: "localhost đã từ chối kết nối"
- Message: "PDF.js Viewer" và "Cloudinary URL" buttons

## 🔍 Nguyên Nhân

Frontend cố gắng load PDF qua endpoint **KHÔNG TỒN TẠI**:
```javascript
const backendProxyUrl = `http://localhost:8080/api/documents/${document.id}/stream`;
```

Backend chưa có endpoint `/api/documents/{id}/stream` này!

## ✅ Giải Pháp

Load PDF **trực tiếp từ Cloudinary URL** (vì đã fix lỗi 401 trước đó).

### File Đã Sửa: `DocumentPreviewModal.jsx`

**Trước:**
```javascript
// Cố load qua backend proxy (không tồn tại)
const backendProxyUrl = `http://localhost:8080/api/documents/${document.id}/stream`;

<iframe src={backendProxyUrl} ... />
```

**Sau:**
```javascript
// Load trực tiếp từ Cloudinary
<iframe src={previewData.previewUrl} ... />
```

---

## 🚀 Deploy Ngay

### Bước 1: Restart Frontend

```bash
# Trong terminal đang chạy frontend, Ctrl+C để stop
# Sau đó start lại:
cd d:\SUPPORT_PROJECT\Phuc_SWP\aistudenthub_FE
npm run dev
```

### Bước 2: Clear Browser Cache

1. Mở DevTools (F12)
2. Right-click vào nút Refresh
3. Chọn "Empty Cache and Hard Reload"

Hoặc:
- Chrome: `Ctrl+Shift+Delete` → Clear cache
- Reload page: `Ctrl+F5`

---

## 🧪 Test Luồng Hoàn Chỉnh

### **Test 1: Upload PDF Mới**

1. Vào http://localhost:5173/documents
2. Click "Tải lên" hoặc "Upload"
3. Chọn file PDF (ví dụ: `test.pdf`)
4. Điền thông tin:
   - Title: "Test PDF"
   - Description: "Testing PDF preview"
5. Click "Upload"

**Expected:** Upload thành công, file xuất hiện trong danh sách

### **Test 2: Preview PDF**

1. Trong danh sách documents, tìm file PDF vừa upload
2. Click nút "Xem trước" (icon mắt 👁️ hoặc button "Preview")
3. Modal mở ra

**Expected:**
- ✅ Iframe hiển thị PDF trực tiếp
- ✅ KHÔNG còn lỗi "localhost đã từ chối kết nối"
- ✅ PDF render trong modal
- ✅ Có nút "Mở PDF.js Viewer" (mở tab mới với viewer nâng cao)
- ✅ Có section mở rộng "Xem nội dung văn bản đã trích xuất"

### **Test 3: Upload Image**

1. Upload file ảnh (JPG/PNG)
2. Click "Xem trước"

**Expected:**
- ✅ Image hiển thị trực tiếp trong modal
- ✅ Không có lỗi load

### **Test 4: Check DevTools Console**

1. Mở DevTools (F12)
2. Tab "Console"
3. Upload và preview file

**Expected:**
- ✅ KHÔNG có lỗi 401
- ✅ KHÔNG có lỗi "refused to connect"
- ✅ Network tab: Request đến Cloudinary URL thành công (200 OK)

---

## 🔍 Troubleshooting

### **Vấn đề 1: Vẫn bị "localhost refused"**

**Nguyên nhân:** Browser cache cũ

**Giải pháp:**
```bash
# 1. Stop frontend (Ctrl+C)
# 2. Clear node_modules cache
cd d:\SUPPORT_PROJECT\Phuc_SWP\aistudenthub_FE
rmdir /s /q .vite
rmdir /s /q dist

# 3. Restart
npm run dev
```

**Hoặc:** Hard reload trong browser (Ctrl+F5)

---

### **Vấn đề 2: PDF không hiển thị (iframe trắng)**

**Nguyên nhân:** PDF trên Cloudinary vẫn là "raw" type (file upload trước khi fix)

**Cách check:**
1. Mở DevTools → Network tab
2. Click "Preview" PDF
3. Tìm request tới Cloudinary
4. Check URL: phải có `/image/upload/`, **KHÔNG phải** `/raw/upload/`

**Giải pháp:**
- Nếu URL có `/raw/upload/` → File cũ, phải **upload lại**
- Nếu URL có `/image/upload/` nhưng vẫn lỗi 401 → Check Cloudinary settings

---

### **Vấn đề 3: Lỗi CORS từ Cloudinary**

**Lỗi:**
```
Access to XMLHttpRequest at 'https://res.cloudinary.com/...' from origin 'http://localhost:5173' 
has been blocked by CORS policy
```

**Giải pháp:** 
- Đây là lỗi hiếm, vì Cloudinary mặc định cho phép CORS
- Click nút "Mở PDF.js Viewer" để xem trong tab mới (không bị CORS)

---

### **Vấn đề 4: Backend chưa có migration**

Nếu upload PDF MỚI mà vẫn bị 401:

**Check backend log:**
```bash
# Tìm dòng log khi upload
grep "Uploading PDF as image type" logs/application.log
```

Nếu KHÔNG thấy → Backend chưa update code

**Giải pháp:**
```bash
cd d:\SUPPORT_PROJECT\Phuc_SWP\aistudenthub

# Rebuild
mvnw clean package -DskipTests

# Restart
mvnw spring-boot:run
```

---

## 📊 Kết Quả Mong Đợi

### **Trước Fix:**
```
User click "Xem trước" 
  → Frontend: iframe src = "http://localhost:8080/api/documents/{id}/stream"
  → Backend: 404 Not Found (endpoint không tồn tại)
  → Browser: "localhost refused to connect" ❌
```

### **Sau Fix:**
```
User click "Xem trước"
  → Frontend: iframe src = "https://res.cloudinary.com/.../image/upload/.../document.pdf"
  → Cloudinary: Return PDF file (200 OK)
  → Browser: Hiển thị PDF trong iframe ✅
```

---

## 🎯 Checklist

- [ ] Frontend code đã update (`DocumentPreviewModal.jsx`)
- [ ] Frontend đã restart (`npm run dev`)
- [ ] Browser cache đã clear (Hard reload: Ctrl+F5)
- [ ] Backend đã có fix 401 (upload PDF với `resource_type: "image"`)
- [ ] Database có cột `storage_resource_type`
- [ ] Upload PDF MỚI để test (PDF cũ sẽ vẫn bị lỗi)

---

## 🎬 Demo Flow Thành Công

1. **Upload:**
   ```
   User → Chọn file PDF → Upload → Success ✅
   Backend log: "Uploading PDF as image type for direct preview: test.pdf"
   Backend log: "Cloudinary upload success: publicId=documents/test_xxx, resourceType=image"
   ```

2. **Preview:**
   ```
   User → Click "Xem trước" → Modal mở
   Iframe load: https://res.cloudinary.com/.../image/upload/.../test.pdf
   Browser: Hiển thị PDF ✅
   ```

3. **Fallback (nếu iframe không load):**
   ```
   User → Click "Mở PDF.js Viewer"
   → Tab mới mở với Mozilla PDF.js viewer
   → PDF hiển thị với toolbar đầy đủ ✅
   ```

---

## 🔗 Links Hữu Ích

- **Frontend:** http://localhost:5173/documents
- **Backend API:** http://localhost:8080/swagger-ui.html
- **Cloudinary Console:** https://cloudinary.com/console/media_library

---

## 📝 Notes

- **PDF.js Viewer button:** Mở PDF trong Mozilla PDF.js viewer (tab mới) với toolbar zoom, search, print
- **Extracted text section:** Hiển thị text đã trích xuất cho AI chatbot (collapsible)
- **Direct Cloudinary URL:** Không cần backend proxy, giảm latency
- **Browser compatibility:** Hầu hết browser hỗ trợ PDF embed (Chrome, Firefox, Edge)

---

**Tóm tắt:** Fix đơn giản - đổi iframe `src` từ backend proxy endpoint (không tồn tại) sang Cloudinary URL trực tiếp! 🚀
