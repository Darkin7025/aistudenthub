# Test File Preview Functionality

## Prerequisites
1. Backend running on `http://localhost:8081`
2. User đã login và có JWT token
3. Test files chuẩn bị sẵn

---

## Test Cases

### 1. Test PDF Preview

**Step 1: Upload PDF**
```bash
curl -X POST http://localhost:8081/api/v1/documents/upload \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "file=@test.pdf" \
  -F 'request={
    "title": "Test PDF Document",
    "description": "PDF preview test",
    "subject": "Testing"
  };type=application/json'
```

**Expected Response:**
```json
{
  "code": 0,
  "message": "Tài liệu đã được upload thành công",
  "data": {
    "id": "uuid-here",
    "title": "Test PDF Document",
    "fileUrl": "https://res.cloudinary.com/.../test.pdf",
    "fileName": "test.pdf",
    "fileType": "application/pdf",
    "previewMode": "PDF",
    "aiSupported": true,
    "uploadStatus": "COMPLETED",
    "uploadProgress": 100
  }
}
```

**Step 2: Get Preview**
```bash
curl -X GET "http://localhost:8081/api/v1/documents/{document-id}/preview" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Expected Preview Response:**
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "documentId": "uuid-here",
    "fileName": "test.pdf",
    "fileType": "application/pdf",
    "previewUrl": "https://res.cloudinary.com/.../test.pdf",
    "previewSupported": true,
    "previewMode": "PDF",
    "textContent": null,
    "truncated": false,
    "aiSupported": true,
    "message": null
  }
}
```

**Verification:**
- ✅ `previewMode` = "PDF"
- ✅ `previewSupported` = true
- ✅ `aiSupported` = true (text extracted)
- ✅ `previewUrl` là Cloudinary URL hợp lệ

---

### 2. Test Image Preview

**Upload & Preview:**
```bash
# Upload
curl -X POST http://localhost:8081/api/v1/documents/upload \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "file=@image.png" \
  -F 'request={
    "title": "Test Image",
    "description": "Image preview test"
  };type=application/json'

# Get preview
curl -X GET "http://localhost:8081/api/v1/documents/{document-id}/preview" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Expected:**
```json
{
  "previewMode": "IMAGE",
  "previewSupported": true,
  "aiSupported": false,
  "previewUrl": "https://res.cloudinary.com/.../image.png"
}
```

**Verification:**
- ✅ `previewMode` = "IMAGE"
- ✅ `aiSupported` = false (no OCR)
- ✅ URL trỏ đến image file

---

### 3. Test Text File Preview

**Upload .txt file:**
```bash
# Create test file
echo "Hello World\nThis is a test text file\nLine 3" > test.txt

# Upload
curl -X POST http://localhost:8081/api/v1/documents/upload \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "file=@test.txt" \
  -F 'request={
    "title": "Test Text File",
    "description": "Text preview test"
  };type=application/json'

# Get preview
curl -X GET "http://localhost:8081/api/v1/documents/{document-id}/preview" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Expected:**
```json
{
  "previewMode": "TEXT",
  "previewSupported": true,
  "aiSupported": true,
  "textContent": "Hello World\nThis is a test text file\nLine 3",
  "truncated": false
}
```

**Verification:**
- ✅ `previewMode` = "TEXT"
- ✅ `textContent` chứa nội dung file
- ✅ `aiSupported` = true
- ✅ `truncated` = false (file nhỏ)

---

### 4. Test Office File Preview

**Upload .docx:**
```bash
curl -X POST http://localhost:8081/api/v1/documents/upload \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "file=@document.docx" \
  -F 'request={
    "title": "Test Word Document",
    "description": "Office preview test"
  };type=application/json'

curl -X GET "http://localhost:8081/api/v1/documents/{document-id}/preview" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Expected:**
```json
{
  "previewMode": "OFFICE",
  "previewSupported": true,
  "aiSupported": false,
  "message": "Office preview uses external viewer. If it cannot load, please download the file."
}
```

**Verification:**
- ✅ `previewMode` = "OFFICE"
- ✅ `previewSupported` = true
- ✅ `message` hướng dẫn sử dụng external viewer

---

### 5. Test Unsupported File

**Upload .zip:**
```bash
curl -X POST http://localhost:8081/api/v1/documents/upload \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "file=@archive.zip" \
  -F 'request={
    "title": "Test Archive",
    "description": "Unsupported preview test"
  };type=application/json'

curl -X GET "http://localhost:8081/api/v1/documents/{document-id}/preview" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Expected:**
```json
{
  "previewMode": "UNSUPPORTED",
  "previewSupported": false,
  "aiSupported": false,
  "message": "Preview is not supported for this file type. Please download the file."
}
```

**Verification:**
- ✅ `previewSupported` = false
- ✅ Message hướng dẫn download

---

### 6. Test Large Text File (Truncation)

**Create large file:**
```bash
# Generate 100,000 chars file
yes "This is line with exactly 80 characters padding to reach the count needed now" | head -n 1250 > large.txt

curl -X POST http://localhost:8081/api/v1/documents/upload \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "file=@large.txt" \
  -F 'request={"title": "Large Text File"};type=application/json'

curl -X GET "http://localhost:8081/api/v1/documents/{document-id}/preview" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Expected:**
```json
{
  "previewMode": "TEXT",
  "textContent": "... first 50,000 chars ...",
  "truncated": true
}
```

**Verification:**
- ✅ `truncated` = true
- ✅ `textContent.length` <= 50,000

---

## Test with Different File Types

### Supported Extensions Test Matrix

| File Type | Extension | Expected Mode | AI Support | Text Extracted |
|-----------|-----------|---------------|------------|----------------|
| PDF | .pdf | PDF | ✅ | ✅ |
| Image | .jpg, .png, .gif | IMAGE | ❌ | ❌ |
| Text | .txt, .md | TEXT | ✅ | ✅ |
| Code | .java, .js, .py | TEXT | ✅ | ✅ |
| Config | .json, .xml, .yml | TEXT | ✅ | ✅ |
| Word | .doc, .docx | OFFICE | ❌ | ❌ |
| Excel | .xls, .xlsx | OFFICE | ❌ | ❌ |
| PowerPoint | .ppt, .pptx | OFFICE | ❌ | ❌ |
| Archive | .zip, .rar, .7z | UNSUPPORTED | ❌ | ❌ |
| Binary | .exe, .jar, .bin | UNSUPPORTED | ❌ | ❌ |

---

## Integration Test Script

```bash
#!/bin/bash

# Config
API_BASE="http://localhost:8081/api/v1"
JWT_TOKEN="your-jwt-token-here"

# Login first to get token
echo "=== 1. Login ==="
LOGIN_RESPONSE=$(curl -s -X POST "$API_BASE/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }')

JWT_TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.data.accessToken')
echo "Token: ${JWT_TOKEN:0:20}..."

# Test PDF
echo -e "\n=== 2. Test PDF Upload & Preview ==="
PDF_UPLOAD=$(curl -s -X POST "$API_BASE/documents/upload" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -F "file=@test.pdf" \
  -F 'request={"title":"Test PDF"};type=application/json')

PDF_ID=$(echo $PDF_UPLOAD | jq -r '.data.id')
echo "Document ID: $PDF_ID"

PDF_PREVIEW=$(curl -s -X GET "$API_BASE/documents/$PDF_ID/preview" \
  -H "Authorization: Bearer $JWT_TOKEN")

echo "Preview Mode: $(echo $PDF_PREVIEW | jq -r '.data.previewMode')"
echo "AI Supported: $(echo $PDF_PREVIEW | jq -r '.data.aiSupported')"

# Test Image
echo -e "\n=== 3. Test Image Upload & Preview ==="
IMG_UPLOAD=$(curl -s -X POST "$API_BASE/documents/upload" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -F "file=@test.png" \
  -F 'request={"title":"Test Image"};type=application/json')

IMG_ID=$(echo $IMG_UPLOAD | jq -r '.data.id')
IMG_PREVIEW=$(curl -s -X GET "$API_BASE/documents/$IMG_ID/preview" \
  -H "Authorization: Bearer $JWT_TOKEN")

echo "Preview Mode: $(echo $IMG_PREVIEW | jq -r '.data.previewMode')"
echo "Preview URL: $(echo $IMG_PREVIEW | jq -r '.data.previewUrl')"

# Test Text
echo -e "\n=== 4. Test Text File Upload & Preview ==="
echo "Sample text content for testing" > test.txt

TXT_UPLOAD=$(curl -s -X POST "$API_BASE/documents/upload" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -F "file=@test.txt" \
  -F 'request={"title":"Test Text"};type=application/json')

TXT_ID=$(echo $TXT_UPLOAD | jq -r '.data.id')
TXT_PREVIEW=$(curl -s -X GET "$API_BASE/documents/$TXT_ID/preview" \
  -H "Authorization: Bearer $JWT_TOKEN")

echo "Preview Mode: $(echo $TXT_PREVIEW | jq -r '.data.previewMode')"
echo "Text Content: $(echo $TXT_PREVIEW | jq -r '.data.textContent')"
echo "AI Supported: $(echo $TXT_PREVIEW | jq -r '.data.aiSupported')"

echo -e "\n=== All Tests Complete ==="
```

---

## PowerShell Test Script (Windows)

```powershell
# Config
$ApiBase = "http://localhost:8081/api/v1"
$Email = "test@example.com"
$Password = "password123"

# Login
Write-Host "=== Login ===" -ForegroundColor Green
$loginBody = @{
    email = $Email
    password = $Password
} | ConvertTo-Json

$loginResponse = Invoke-RestMethod -Uri "$ApiBase/auth/login" `
    -Method Post `
    -Body $loginBody `
    -ContentType "application/json"

$token = $loginResponse.data.accessToken
Write-Host "Token obtained: $($token.Substring(0,20))..."

# Test PDF Upload
Write-Host "`n=== Test PDF Upload ===" -ForegroundColor Green
$pdfForm = @{
    file = Get-Item -Path "test.pdf"
    request = @{
        title = "Test PDF Document"
        description = "Testing PDF preview"
    } | ConvertTo-Json
}

$headers = @{
    Authorization = "Bearer $token"
}

$pdfUpload = Invoke-RestMethod -Uri "$ApiBase/documents/upload" `
    -Method Post `
    -Headers $headers `
    -Form $pdfForm

$docId = $pdfUpload.data.id
Write-Host "Document uploaded: $docId"

# Get Preview
Write-Host "`n=== Get Preview ===" -ForegroundColor Green
$preview = Invoke-RestMethod -Uri "$ApiBase/documents/$docId/preview" `
    -Method Get `
    -Headers $headers

Write-Host "Preview Mode: $($preview.data.previewMode)"
Write-Host "AI Supported: $($preview.data.aiSupported)"
Write-Host "Preview URL: $($preview.data.previewUrl)"
```

---

## Manual Testing Checklist

### Upload Phase
- [ ] File validates (size, type)
- [ ] Upload returns COMPLETED status
- [ ] Cloudinary URL accessible
- [ ] Metadata saved correctly

### Preview Phase  
- [ ] Preview endpoint requires auth
- [ ] Returns correct previewMode
- [ ] previewUrl is valid Cloudinary link
- [ ] textContent present for TEXT/PDF
- [ ] aiSupported flag correct

### Frontend Display
- [ ] PDF renders in PDF.js/iframe
- [ ] Images display correctly
- [ ] Text shows with syntax highlighting
- [ ] Office files open in external viewer
- [ ] Unsupported shows download button
- [ ] AI button only shows when supported

---

## Common Issues & Solutions

### Issue: Preview URL 404
**Cause:** File chưa upload xong hoặc Cloudinary URL sai  
**Fix:** Check uploadStatus = COMPLETED trước khi preview

### Issue: PDF không extract được text
**Cause:** PDF dạng scan/image không có text layer  
**Fix:** Implement OCR (future) hoặc show warning

### Issue: Office file không preview được
**Cause:** Google Docs Viewer rate limit  
**Fix:** Fallback to download button

### Issue: textContent null dù file là TEXT
**Cause:** Encoding issue hoặc file binary  
**Fix:** Check file.getContentType() và encoding

---

## Performance Benchmarks

| File Type | Size | Upload Time | Preview Load | Text Extract |
|-----------|------|-------------|--------------|--------------|
| PDF | 1MB | ~2s | <500ms | ~1s |
| Image | 500KB | ~1s | <200ms | N/A |
| Text | 100KB | <1s | <100ms | Instant |
| Office | 2MB | ~3s | ~2s (external) | N/A |

---

**Next Steps:**
1. Run automated test script
2. Verify each preview mode works
3. Check AI chat integration with extracted text
4. Test edge cases (empty files, huge files, corrupt files)
