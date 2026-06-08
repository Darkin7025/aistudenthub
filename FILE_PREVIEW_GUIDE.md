# 📄 File Preview Guide - AI Student Hub

## ✅ Preview Feature Status

**Đã implement hoàn chỉnh!** Hệ thống hỗ trợ preview cho tất cả file types phổ biến.

---

## 🎯 Preview API Endpoint

### GET `/api/v1/documents/{id}/preview`

**Headers:**
```
Authorization: Bearer {JWT_TOKEN}
```

**Response Structure:**
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "documentId": "uuid",
    "fileName": "example.pdf",
    "fileType": "application/pdf",
    "previewUrl": "https://res.cloudinary.com/...",
    "previewSupported": true,
    "previewMode": "PDF",
    "textContent": null,
    "truncated": false,
    "aiSupported": true,
    "message": null
  }
}
```

---

## 📋 Supported Preview Modes

### 1. 📄 PDF Files

**Extensions:** `.pdf`  
**Preview Mode:** `PDF`  
**AI Support:** ✅ Yes (text extracted during upload)

**Backend:**
- Text được extract bằng Apache PDFBox khi upload
- Lưu vào `extractedText` field để phục vụ AI/RAG

**Frontend Options:**

**Option A: PDF.js (recommended)**
```jsx
import { Document, Page } from 'react-pdf';

<Document file={previewUrl}>
  <Page pageNumber={1} />
</Document>
```

**Option B: Cloudinary Transformation**
```jsx
// Convert first page to image
const thumbnailUrl = previewUrl.replace('/upload/', '/upload/pg_1,w_800/');
<img src={thumbnailUrl} alt="PDF Preview" />
```

**Option C: Iframe (simple)**
```jsx
<iframe 
  src={previewUrl} 
  width="100%" 
  height="600px"
  title="PDF Preview"
/>
```

---

### 2. 🖼️ Image Files

**Extensions:** `.jpg`, `.jpeg`, `.png`, `.gif`, `.webp`, `.bmp`  
**MIME Types:** `image/*`  
**Preview Mode:** `IMAGE`  
**AI Support:** ❌ No

**Frontend:**
```jsx
<img src={previewUrl} alt={fileName} />
```

**Cloudinary Transformations:**
```jsx
// Thumbnail 300x300
const thumbnail = previewUrl.replace('/upload/', '/upload/w_300,h_300,c_fill/');

// Responsive width
const responsive = previewUrl.replace('/upload/', '/upload/w_800,c_limit/');

// Add effects
const blurred = previewUrl.replace('/upload/', '/upload/e_blur:300/');
```

---

### 3. 📝 Text Files

**Extensions:** `.txt`, `.md`, `.csv`, `.json`, `.xml`, `.yml`, `.yaml`, `.sql`, `.java`, `.js`, `.jsx`, `.ts`, `.tsx`, `.py`, `.dart`, `.html`, `.css`, `.scss`, `.log`, `.properties`, `.env`, `.gitignore`  
**MIME Types:** `text/*`, `application/json`, `application/xml`  
**Preview Mode:** `TEXT`  
**AI Support:** ✅ Yes

**Backend:**
- Full text content được trả về trong `textContent` field
- Limit 50,000 ký tự (nếu file lớn hơn → `truncated: true`)

**Frontend:**
```jsx
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';

{previewMode === 'TEXT' && (
  <SyntaxHighlighter language="javascript">
    {textContent}
  </SyntaxHighlighter>
)}
```

---

### 4. 📊 Office Files (Word, Excel, PowerPoint)

**Extensions:** `.doc`, `.docx`, `.ppt`, `.pptx`, `.xls`, `.xlsx`  
**MIME Types:** `application/msword`, `application/vnd.openxmlformats-officedocument.*`  
**Preview Mode:** `OFFICE`  
**AI Support:** ❌ No

**Backend Message:**
```
"Office preview uses external viewer. If it cannot load, please download the file."
```

**Frontend Options:**

**Option A: Google Docs Viewer**
```jsx
const googleViewerUrl = `https://docs.google.com/viewer?url=${encodeURIComponent(previewUrl)}&embedded=true`;

<iframe 
  src={googleViewerUrl}
  width="100%"
  height="600px"
  title="Office Preview"
/>
```

**Option B: Microsoft Office Online**
```jsx
const officeViewerUrl = `https://view.officeapps.live.com/op/embed.aspx?src=${encodeURIComponent(previewUrl)}`;

<iframe 
  src={officeViewerUrl}
  width="100%"
  height="600px"
  title="Office Preview"
/>
```

**Option C: Cloudinary Office Conversion (premium)**
```jsx
// Convert to PDF first (requires Cloudinary add-on)
const pdfUrl = previewUrl.replace('.docx', '.pdf');
```

---

### 5. ❌ Unsupported Files

**Extensions:** `.zip`, `.rar`, `.7z`, `.exe`, `.jar`, `.bin`, `.apk`, `.iso`  
**Preview Mode:** `UNSUPPORTED`  
**AI Support:** ❌ No

**Backend Message:**
```
"Preview is not supported for this file type. Please download the file."
```

**Frontend:**
```jsx
{!previewSupported && (
  <div className="preview-unsupported">
    <FileIcon />
    <p>{message}</p>
    <button onClick={() => downloadFile(downloadUrl)}>
      Tải xuống
    </button>
  </div>
)}
```

---

## 🎨 Complete Frontend Preview Component Example

```jsx
import React from 'react';
import { Document, Page } from 'react-pdf';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';

function DocumentPreview({ documentId }) {
  const [preview, setPreview] = React.useState(null);
  const [loading, setLoading] = React.useState(true);

  React.useEffect(() => {
    fetch(`/api/v1/documents/${documentId}/preview`, {
      headers: {
        'Authorization': `Bearer ${getToken()}`
      }
    })
    .then(res => res.json())
    .then(data => {
      setPreview(data.data);
      setLoading(false);
    });
  }, [documentId]);

  if (loading) return <div>Loading preview...</div>;
  if (!preview.previewSupported) {
    return (
      <div className="preview-unsupported">
        <p>{preview.message}</p>
        <a href={preview.previewUrl} download>Tải xuống file</a>
      </div>
    );
  }

  switch (preview.previewMode) {
    case 'PDF':
      return (
        <Document file={preview.previewUrl}>
          <Page pageNumber={1} width={800} />
        </Document>
      );

    case 'IMAGE':
      return (
        <img 
          src={preview.previewUrl} 
          alt={preview.fileName}
          style={{ maxWidth: '100%' }}
        />
      );

    case 'TEXT':
      return (
        <div>
          <SyntaxHighlighter language="javascript">
            {preview.textContent}
          </SyntaxHighlighter>
          {preview.truncated && (
            <p>⚠️ File quá lớn, chỉ hiển thị 50,000 ký tự đầu</p>
          )}
        </div>
      );

    case 'OFFICE':
      const googleViewerUrl = `https://docs.google.com/viewer?url=${encodeURIComponent(preview.previewUrl)}&embedded=true`;
      return (
        <div>
          {preview.message && <p className="info">{preview.message}</p>}
          <iframe 
            src={googleViewerUrl}
            width="100%"
            height="600px"
            title="Office Preview"
          />
        </div>
      );

    default:
      return <div>Unknown preview mode</div>;
  }
}
```

---

## 📱 Preview After Upload Flow

### Step 1: Upload Document
```bash
curl -X POST http://localhost:8081/api/v1/documents/upload \
  -H "Authorization: Bearer {JWT}" \
  -F "file=@example.pdf" \
  -F 'request={
    "title": "Example Document",
    "description": "Test file",
    "subject": "Computer Science"
  };type=application/json'
```

**Response:**
```json
{
  "data": {
    "id": "doc-uuid",
    "title": "Example Document",
    "fileUrl": "https://res.cloudinary.com/.../example.pdf",
    "fileName": "example.pdf",
    "fileType": "application/pdf",
    "previewMode": "PDF",
    "aiSupported": true,
    "uploadStatus": "COMPLETED",
    "uploadProgress": 100
  }
}
```

### Step 2: Get Preview
```bash
curl -X GET http://localhost:8081/api/v1/documents/{doc-uuid}/preview \
  -H "Authorization: Bearer {JWT}"
```

### Step 3: Display in Frontend
- Use `previewMode` to determine which component to render
- Show `previewUrl` via appropriate viewer
- Display `message` if present
- Show AI chat button if `aiSupported === true`

---

## 🔍 Preview Mode Detection Logic

**Backend Implementation** (`DocumentPreviewResolver.java`):

1. **Check extension first**
   - `.pdf` → PDF
   - `.jpg`, `.png`, etc. → IMAGE
   - `.txt`, `.md`, `.json`, etc. → TEXT
   - `.doc`, `.docx`, etc. → OFFICE
   - `.zip`, `.exe`, etc. → UNSUPPORTED

2. **Fallback to MIME type**
   - `application/pdf` → PDF
   - `image/*` → IMAGE
   - `text/*` → TEXT
   - Office MIME types → OFFICE

3. **Default: UNSUPPORTED**

---

## 🤖 AI/RAG Support

**Files with AI Support:**
- ✅ PDF (text extracted via PDFBox)
- ✅ TEXT (full content stored)
- ❌ IMAGE (no OCR yet)
- ❌ OFFICE (no extraction yet)

**Frontend:**
```jsx
{preview.aiSupported && (
  <button onClick={() => openChatWithDocument(documentId)}>
    💬 Hỏi AI về tài liệu này
  </button>
)}
```

**Chat API:**
```bash
POST /api/v1/chat/document/{documentId}
{
  "message": "Tóm tắt nội dung chính của tài liệu"
}
```

---

## 📦 Required Dependencies

### Backend (Already Installed)
```xml
<!-- pom.xml -->
<dependency>
    <groupId>com.cloudinary</groupId>
    <artifactId>cloudinary-http44</artifactId>
</dependency>
<dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>pdfbox</artifactId>
</dependency>
```

### Frontend (Need to Install)
```bash
# For PDF preview
npm install react-pdf

# For syntax highlighting (text files)
npm install react-syntax-highlighter

# For Office preview - no additional package needed (iframe)
```

---

## ⚡ Performance Optimization

### 1. Image Thumbnails
```jsx
// Generate thumbnail on-the-fly with Cloudinary
const thumbnail = fileUrl.replace('/upload/', '/upload/w_300,h_300,c_fill/');
const fullSize = fileUrl; // load on click
```

### 2. PDF Lazy Loading
```jsx
<Document 
  file={previewUrl}
  loading={<Spinner />}
  onLoadError={(error) => console.error('PDF load error:', error)}
>
  <Page pageNumber={currentPage} />
</Document>
```

### 3. Text Content Caching
```jsx
// Cache textContent in localStorage/Redux
const cachedContent = localStorage.getItem(`doc-${documentId}`);
if (cachedContent) {
  setTextContent(JSON.parse(cachedContent));
} else {
  fetchPreview();
}
```

---

## 🐛 Troubleshooting

### PDF không hiển thị
- Check CORS: Cloudinary URL phải allow CORS
- Try iframe fallback nếu react-pdf fail

### Office preview không load
- Google Docs Viewer yêu cầu file URL public
- Microsoft Office Online có rate limits
- Fallback: download button

### Text quá dài bị truncate
- Backend limit: 50,000 chars
- Frontend: implement "Load more" button
- Gọi download API để lấy full content

### AI không support file
- Check `aiSupported` flag trong response
- Chỉ PDF và TEXT được extract text
- Office/Image cần implement OCR/extraction riêng

---

## 🎯 Testing Checklist

- [ ] Upload PDF → xem preview → text extracted
- [ ] Upload image → xem preview trực tiếp
- [ ] Upload .txt/.md → xem text content
- [ ] Upload .docx → xem qua Google Docs Viewer
- [ ] Upload .zip → message "unsupported"
- [ ] Check AI button chỉ hiện với PDF/TEXT
- [ ] Test download button với mọi file types
- [ ] Verify JWT auth trên mọi endpoints

---

## 📚 API Reference Summary

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/v1/documents/upload` | POST | Upload file + metadata |
| `/api/v1/documents/{id}` | GET | Get document details |
| `/api/v1/documents/{id}/preview` | GET | **Get preview data** |
| `/api/v1/documents/{id}/download` | GET | Get download URL |
| `/api/v1/documents/{id}/upload-status` | GET | Check upload progress |
| `/api/v1/chat/document/{id}` | POST | Chat with AI about document |

---

**Tóm lại:** Backend đã sẵn sàng cho preview toàn bộ file types. Frontend chỉ cần:
1. Call `/preview` endpoint
2. Check `previewMode`
3. Render component tương ứng (PDF.js, img tag, syntax highlighter, iframe)
4. Handle unsupported cases với download button
