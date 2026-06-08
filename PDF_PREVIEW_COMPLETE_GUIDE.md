# 📄 PDF Preview - Complete Implementation & Testing Guide

## ✅ Backend Implementation

### 1. Upload Flow (ALREADY CORRECT)

**File:** `DocumentService.java` (lines 58-115)

**What it does:**
```java
// 1. Validate file
validateFile(file);

// 2. Extract text from PDF (if possible)
if (PreviewMode.PDF.equals(previewMode)) {
    PDDocument pdDoc = Loader.loadPDF(file.getBytes());
    extractedText = new PDFTextStripper().getText(pdDoc);
}

// 3. Upload to Cloudinary
Map<String, String> uploadResult = cloudinaryService.upload(file);

// 4. Save to database
Document doc = Document.builder()
    .fileName(file.getOriginalFilename())
    .fileType(file.getContentType())  // "application/pdf"
    .fileUrl(uploadResult.get("url")) // Cloudinary secure_url
    .uploadStatus(UploadStatus.COMPLETED) // Default
    .extractedText(extractedText)
    .build();
```

**What is saved:**
- ✅ `fileName` - lab1.pdf
- ✅ `fileType` - application/pdf
- ✅ `fileUrl` - https://res.cloudinary.com/.../lab1.pdf
- ✅ `uploadStatus` - COMPLETED
- ✅ `extractedText` - Text content (if extractable)

---

### 2. Preview API (ENHANCED)

**Endpoint:** `GET /api/v1/documents/{id}/preview`

**File:** `DocumentService.java` (lines 246-320)

**Logic Flow:**
```java
public PreviewResponse getPreview(UUID documentId, User currentUser) {
    // 1. Load document
    Document doc = documentRepository.findById(documentId)
        .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND));
    
    // 2. Check permission (IMPORTANT!)
    if (!canPreviewDocument(doc, currentUser)) {
        throw new AppException(ErrorCode.FORBIDDEN_ACCESS);
    }
    
    // 3. Determine preview mode
    PreviewMode previewMode = previewResolver.resolveMode(fileName, doc.getFileType());
    
    // 4. Handle PDF specifically
    if (PreviewMode.PDF.equals(previewMode)) {
        if (!StringUtils.hasText(previewUrl)) {
            previewSupported = false;
            message = "File PDF không có URL để xem trước";
        } else {
            previewSupported = true;
            message = "Xem PDF trực tiếp...";
        }
    }
    
    // 5. Return response
    return PreviewResponse.builder()
        .documentId(doc.getId())
        .fileName(fileName)
        .fileType(doc.getFileType())
        .previewUrl(previewUrl)          // Cloudinary URL
        .previewMode("PDF")
        .previewSupported(true)
        .textContent(extractedText)       // Optional
        .aiSupported(hasText)
        .message(message)
        .build();
}
```

**Permission Check:**
```java
private boolean canPreviewDocument(Document doc, User currentUser) {
    return doc.getUserId().equals(currentUser.getId()) 
        || Role.ADMIN.equals(currentUser.getRole());
}
```

---

### 3. Preview Response Structure

**Success Response (PDF with text):**
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "documentId": "550e8400-e29b-41d4-a716-446655440000",
    "fileName": "lab1.pdf",
    "fileType": "application/pdf",
    "previewUrl": "https://res.cloudinary.com/demo/image/upload/v1234567890/documents/lab1.pdf",
    "previewSupported": true,
    "previewMode": "PDF",
    "textContent": "Extracted text content here...",
    "truncated": false,
    "aiSupported": true,
    "message": "Bạn có thể xem PDF trực tiếp hoặc đọc nội dung văn bản bên dưới."
  }
}
```

**Success Response (PDF scan - no text):**
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "documentId": "550e8400-e29b-41d4-a716-446655440000",
    "fileName": "scanned.pdf",
    "fileType": "application/pdf",
    "previewUrl": "https://res.cloudinary.com/demo/image/upload/v1234567890/documents/scanned.pdf",
    "previewSupported": true,
    "previewMode": "PDF",
    "textContent": null,
    "truncated": false,
    "aiSupported": false,
    "message": "Xem PDF trực tiếp. File không chứa text có thể trích xuất (có thể là scan ảnh)."
  }
}
```

**Error Response (No URL):**
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "documentId": "550e8400-e29b-41d4-a716-446655440000",
    "fileName": "broken.pdf",
    "fileType": "application/pdf",
    "previewUrl": null,
    "previewSupported": false,
    "previewMode": "PDF",
    "textContent": null,
    "truncated": false,
    "aiSupported": false,
    "message": "File PDF không có URL để xem trước. Vui lòng liên hệ quản trị viên."
  }
}
```

**Error Response (No Permission):**
```json
{
  "code": 1003,
  "message": "Bạn không có quyền truy cập tài nguyên này",
  "data": null
}
```

---

## 🎨 Frontend Implementation

### DocumentPreviewModal.jsx (RECOMMENDED IMPLEMENTATION)

```jsx
import React, { useState, useEffect } from 'react';
import './DocumentPreviewModal.css';

function DocumentPreviewModal({ documentId, isOpen, onClose, token }) {
  const [preview, setPreview] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!isOpen || !documentId) return;

    fetchPreview();
  }, [isOpen, documentId]);

  const fetchPreview = async () => {
    setLoading(true);
    setError(null);

    try {
      const response = await fetch(
        `http://localhost:8081/api/v1/documents/${documentId}/preview`,
        {
          headers: {
            'Authorization': `Bearer ${token}`
          }
        }
      );

      if (!response.ok) {
        if (response.status === 403) {
          throw new Error('Bạn không có quyền xem tài liệu này');
        }
        throw new Error('Không thể tải preview');
      }

      const data = await response.json();
      setPreview(data.data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleDownload = () => {
    if (preview?.previewUrl) {
      window.open(preview.previewUrl, '_blank');
    }
  };

  if (!isOpen) return null;

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        {/* Header */}
        <div className="modal-header">
          <h2>{preview?.fileName || 'Xem trước tài liệu'}</h2>
          <button className="close-btn" onClick={onClose}>×</button>
        </div>

        {/* Body */}
        <div className="modal-body">
          {loading && <div className="loading">Đang tải...</div>}

          {error && (
            <div className="error-message">
              <p>⚠️ {error}</p>
              <button onClick={onClose}>Đóng</button>
            </div>
          )}

          {!loading && !error && preview && (
            <>
              {/* Info Message */}
              {preview.message && (
                <div className={`info-banner ${preview.previewSupported ? 'success' : 'warning'}`}>
                  {preview.message}
                </div>
              )}

              {/* PDF Preview */}
              {preview.previewMode === 'PDF' && preview.previewSupported && (
                <div className="pdf-preview-container">
                  <iframe
                    src={preview.previewUrl}
                    title={preview.fileName}
                    width="100%"
                    height="75vh"
                    style={{
                      border: '1px solid #ddd',
                      borderRadius: '4px'
                    }}
                  />
                  
                  {/* Download Button */}
                  <div className="preview-actions">
                    <button 
                      className="download-btn"
                      onClick={handleDownload}
                    >
                      📥 Tải xuống PDF
                    </button>
                  </div>

                  {/* Text Content (if available) */}
                  {preview.textContent && (
                    <details className="text-content-toggle">
                      <summary>📝 Xem nội dung văn bản</summary>
                      <div className="text-content">
                        <pre>{preview.textContent}</pre>
                        {preview.truncated && (
                          <p className="truncate-notice">
                            ⚠️ Chỉ hiển thị 50,000 ký tự đầu tiên
                          </p>
                        )}
                      </div>
                    </details>
                  )}

                  {/* AI Chat Button (if supported) */}
                  {preview.aiSupported && (
                    <button 
                      className="ai-chat-btn"
                      onClick={() => openAIChat(documentId)}
                    >
                      💬 Hỏi AI về tài liệu này
                    </button>
                  )}
                </div>
              )}

              {/* IMAGE Preview */}
              {preview.previewMode === 'IMAGE' && preview.previewSupported && (
                <div className="image-preview">
                  <img 
                    src={preview.previewUrl} 
                    alt={preview.fileName}
                    style={{ maxWidth: '100%', height: 'auto' }}
                  />
                  <button onClick={handleDownload}>📥 Tải xuống</button>
                </div>
              )}

              {/* TEXT Preview */}
              {preview.previewMode === 'TEXT' && preview.previewSupported && (
                <div className="text-preview">
                  <pre>{preview.textContent}</pre>
                  {preview.truncated && (
                    <p>⚠️ Chỉ hiển thị 50,000 ký tự đầu</p>
                  )}
                </div>
              )}

              {/* OFFICE Preview */}
              {preview.previewMode === 'OFFICE' && preview.previewSupported && (
                <div className="office-preview">
                  <iframe
                    src={`https://docs.google.com/viewer?url=${encodeURIComponent(preview.previewUrl)}&embedded=true`}
                    width="100%"
                    height="75vh"
                    title={preview.fileName}
                  />
                  <button onClick={handleDownload}>📥 Tải xuống</button>
                </div>
              )}

              {/* UNSUPPORTED */}
              {!preview.previewSupported && (
                <div className="unsupported-preview">
                  <p>❌ {preview.message || 'Không thể xem trước file này'}</p>
                  <button onClick={handleDownload}>📥 Tải xuống file</button>
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
}

export default DocumentPreviewModal;
```

### CSS (DocumentPreviewModal.css)

```css
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 8px;
  width: 90%;
  max-width: 1200px;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #ddd;
}

.modal-header h2 {
  margin: 0;
  font-size: 20px;
}

.close-btn {
  background: none;
  border: none;
  font-size: 32px;
  cursor: pointer;
  color: #666;
}

.close-btn:hover {
  color: #000;
}

.modal-body {
  padding: 20px;
  overflow-y: auto;
  flex: 1;
}

.info-banner {
  padding: 12px;
  margin-bottom: 16px;
  border-radius: 4px;
  background: #e3f2fd;
  border-left: 4px solid #2196f3;
}

.info-banner.success {
  background: #e8f5e9;
  border-left-color: #4caf50;
}

.info-banner.warning {
  background: #fff3e0;
  border-left-color: #ff9800;
}

.pdf-preview-container,
.image-preview,
.text-preview,
.office-preview {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.preview-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  margin-top: 16px;
}

.download-btn,
.ai-chat-btn {
  padding: 10px 20px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: background 0.2s;
}

.download-btn {
  background: #2196f3;
  color: white;
}

.download-btn:hover {
  background: #1976d2;
}

.ai-chat-btn {
  background: #4caf50;
  color: white;
}

.ai-chat-btn:hover {
  background: #388e3c;
}

.text-content-toggle {
  margin-top: 16px;
  border: 1px solid #ddd;
  border-radius: 4px;
  padding: 12px;
}

.text-content-toggle summary {
  cursor: pointer;
  font-weight: 500;
  user-select: none;
}

.text-content {
  margin-top: 12px;
  max-height: 300px;
  overflow-y: auto;
  background: #f5f5f5;
  padding: 12px;
  border-radius: 4px;
}

.text-content pre {
  white-space: pre-wrap;
  word-break: break-word;
  margin: 0;
  font-size: 13px;
}

.truncate-notice {
  margin-top: 8px;
  color: #ff9800;
  font-size: 13px;
}

.loading {
  text-align: center;
  padding: 40px;
  font-size: 16px;
  color: #666;
}

.error-message {
  text-align: center;
  padding: 40px;
}

.error-message p {
  color: #f44336;
  font-size: 16px;
  margin-bottom: 16px;
}

.unsupported-preview {
  text-align: center;
  padding: 40px;
}

.unsupported-preview p {
  font-size: 16px;
  margin-bottom: 16px;
  color: #666;
}
```

---

## 🧪 Testing Guide

### 1. Backend Testing with Postman

#### Step 1: Login
```http
POST http://localhost:8081/api/v1/auth/login
Content-Type: application/json

{
  "email": "test@example.com",
  "password": "password123"
}
```

**Save the `accessToken` from response.**

---

#### Step 2: Upload PDF
```http
POST http://localhost:8081/api/v1/documents/upload
Authorization: Bearer YOUR_ACCESS_TOKEN
Content-Type: multipart/form-data

Body (form-data):
- file: [Select lab1.pdf]
- request: {
    "title": "Lab 1 PDF Test",
    "description": "Testing PDF preview"
  }
```

**Save the `id` from response.**

**Expected Response:**
```json
{
  "code": 0,
  "message": "Tài liệu đã được upload thành công",
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "title": "Lab 1 PDF Test",
    "fileName": "lab1.pdf",
    "fileType": "application/pdf",
    "fileUrl": "https://res.cloudinary.com/demo/image/upload/.../lab1.pdf",
    "uploadStatus": "COMPLETED",
    "uploadProgress": 100,
    "previewMode": "PDF",
    "aiSupported": true
  }
}
```

---

#### Step 3: Verify fileUrl in Database
```sql
SELECT 
    id,
    title,
    file_name,
    file_type,
    file_url,
    upload_status
FROM documents
WHERE id = '550e8400-e29b-41d4-a716-446655440000';
```

**Check:**
- ✅ `file_url` contains Cloudinary URL
- ✅ `file_type` = 'application/pdf'
- ✅ `upload_status` = 'COMPLETED'

---

#### Step 4: Test fileUrl in Browser
Copy the `file_url` from database and open in new tab:
```
https://res.cloudinary.com/demo/image/upload/.../lab1.pdf
```

**Expected:** PDF opens and displays correctly.

---

#### Step 5: Get Preview (Owner)
```http
GET http://localhost:8081/api/v1/documents/550e8400-e29b-41d4-a716-446655440000/preview
Authorization: Bearer YOUR_ACCESS_TOKEN
```

**Expected Response:**
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "documentId": "550e8400-e29b-41d4-a716-446655440000",
    "fileName": "lab1.pdf",
    "fileType": "application/pdf",
    "previewUrl": "https://res.cloudinary.com/.../lab1.pdf",
    "previewSupported": true,
    "previewMode": "PDF",
    "textContent": "Extracted text if available...",
    "truncated": false,
    "aiSupported": true,
    "message": "Bạn có thể xem PDF trực tiếp hoặc đọc nội dung văn bản bên dưới."
  }
}
```

---

#### Step 6: Test Permission (Different User)

Login as different user and try to preview:

```http
GET http://localhost:8081/api/v1/documents/550e8400-e29b-41d4-a716-446655440000/preview
Authorization: Bearer DIFFERENT_USER_TOKEN
```

**Expected Response:**
```json
{
  "code": 1003,
  "message": "Bạn không có quyền truy cập tài nguyên này",
  "data": null
}
```

---

### 2. Frontend Testing

#### Step 1: Open Application
```
http://localhost:5173 (or your frontend port)
```

#### Step 2: Login
Login with your test account.

#### Step 3: Upload PDF
1. Click "Upload Document"
2. Select `lab1.pdf`
3. Fill title and description
4. Click "Upload"
5. Wait for success message

#### Step 4: Open Preview
1. Find the uploaded document in list
2. Click "Xem trước" button
3. Modal should open

**Expected Behavior:**
- ✅ Modal opens with document name in header
- ✅ Info banner shows message
- ✅ iframe displays PDF content
- ✅ Download button is visible
- ✅ AI chat button shows (if aiSupported = true)
- ✅ Text content toggle shows (if text was extracted)

#### Step 5: Test iframe
- PDF should render inside iframe
- Can scroll through pages
- Quality should be good

#### Step 6: Test Download
- Click "📥 Tải xuống PDF" button
- New tab opens with PDF
- Can download from new tab

#### Step 7: Test Text Content (if available)
- Click "📝 Xem nội dung văn bản"
- Text content expands
- Should show extracted text

#### Step 8: Test AI Chat (if supported)
- Click "💬 Hỏi AI về tài liệu này"
- Should open chat interface

---

### 3. Edge Cases Testing

#### Test Case 1: PDF without Text (Scanned)
1. Upload scanned PDF (image-only)
2. Preview should show iframe
3. Message: "Xem PDF trực tiếp. File không chứa text..."
4. No AI chat button
5. Download button works

#### Test Case 2: Large PDF
1. Upload PDF > 100 pages
2. iframe should load all pages
3. May take longer to load
4. Scrolling should work smoothly

#### Test Case 3: Corrupted Upload
1. Simulate upload failure (disconnect network mid-upload)
2. fileUrl should be null
3. Preview API should return previewSupported = false
4. Message: "File PDF không có URL để xem trước"

#### Test Case 4: Permission Denied
1. User A uploads PDF
2. User B tries to preview (different account)
3. Should get 403 Forbidden
4. Frontend should show error message
5. Modal should not crash

---

## 📋 Checklist

### Backend ✅
- [x] Upload saves fileName correctly
- [x] Upload saves fileType = "application/pdf"
- [x] Upload saves fileUrl from Cloudinary
- [x] Upload sets uploadStatus = COMPLETED
- [x] Preview API checks permissions
- [x] Preview API detects PDF by MIME/extension
- [x] Preview API returns previewMode = "PDF"
- [x] Preview API returns previewSupported = true
- [x] Preview API returns valid previewUrl
- [x] Preview API handles null fileUrl gracefully
- [x] Preview API extracts text if possible

### Frontend (TODO)
- [ ] DocumentPreviewModal component created
- [ ] Modal opens on "Xem trước" click
- [ ] iframe renders PDF from previewUrl
- [ ] iframe height = 75vh
- [ ] Download button works
- [ ] Text content toggle works (if available)
- [ ] AI chat button shows (if supported)
- [ ] Error handling for 403 Forbidden
- [ ] Loading state while fetching preview
- [ ] Modal closes properly

---

## 🚀 Deployment Notes

### Environment Variables
```properties
# Backend (application.properties)
cloudinary.cloud-name=YOUR_CLOUD_NAME
cloudinary.api-key=YOUR_API_KEY
cloudinary.api-secret=YOUR_API_SECRET
```

### CORS Configuration
Ensure Cloudinary URLs are accessible:
```java
@Configuration
public class CorsConfig {
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOrigin("http://localhost:5173");
        config.addAllowedOrigin("https://your-production-domain.com");
        // ... other configs
        return source;
    }
}
```

### Security Headers
If iframe doesn't load, check:
```
X-Frame-Options: SAMEORIGIN
Content-Security-Policy: frame-src 'self' https://res.cloudinary.com
```

---

## 📞 Troubleshooting

### Problem: iframe shows blank
**Cause:** CORS or X-Frame-Options blocking  
**Fix:** Cloudinary should allow embedding. Test URL in new tab first.

### Problem: Preview API returns 403
**Cause:** User doesn't own document  
**Fix:** Check userId in database matches logged-in user

### Problem: fileUrl is null
**Cause:** Cloudinary upload failed  
**Fix:** Check Cloudinary credentials, network connection

### Problem: PDF text not extracted
**Cause:** PDF is scanned image, not text-based  
**Fix:** Normal behavior, iframe preview still works

### Problem: Large PDF slow to load
**Cause:** File size > 10MB  
**Fix:** Consider Cloudinary transformations or lazy loading

---

## ✅ Summary

**Backend Changes:**
- Enhanced PDF preview logic to check fileUrl
- Better error messages
- Permission validation working

**Frontend Needed:**
- Implement DocumentPreviewModal component
- iframe with height 75vh
- Download button
- Text content toggle
- AI chat button (conditional)

**Testing:**
- Postman for API
- Database verification
- Browser testing
- Permission testing
- Edge cases

**Result:** Complete PDF preview system with proper security and UX! 🎉
