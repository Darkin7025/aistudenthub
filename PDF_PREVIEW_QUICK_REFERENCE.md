# 📄 PDF Preview - Quick Reference Card

## 🎯 One-Page Summary

### Backend (Already Done ✅)

**Upload saves:**
- fileName: `lab1.pdf`
- fileType: `application/pdf`
- fileUrl: `https://res.cloudinary.com/.../lab1.pdf`
- uploadStatus: `COMPLETED`

**Preview API:**
```
GET /api/v1/documents/{id}/preview
Authorization: Bearer {JWT}

→ Returns:
{
  "previewMode": "PDF",
  "previewSupported": true,
  "previewUrl": "https://cloudinary.com/.../lab1.pdf",
  "textContent": "..." or null,
  "aiSupported": true/false,
  "message": "Helpful message"
}
```

**Security:** ✅ Only owner/admin can preview

---

### Frontend (Need to Implement)

**Component:**
```jsx
// DocumentPreviewModal.jsx
<iframe 
  src={preview.previewUrl}
  width="100%"
  height="75vh"
/>

<button onClick={() => window.open(preview.previewUrl)}>
  📥 Download
</button>

{preview.textContent && (
  <details>
    <summary>📝 Text</summary>
    <pre>{preview.textContent}</pre>
  </details>
)}

{preview.aiSupported && (
  <button>💬 AI Chat</button>
)}
```

---

### Testing

**Postman:**
1. Import `PDF_Preview_Test.postman_collection.json`
2. Run "Login" → "Upload PDF" → "Get Preview"
3. Check console for URLs to test

**Browser:**
1. Upload lab1.pdf
2. Click "Xem trước"
3. Should see PDF in iframe

**Database:**
```sql
SELECT file_url, file_type 
FROM documents 
WHERE file_name = 'lab1.pdf';
```
Check: file_url has Cloudinary URL

---

### Response Examples

**Success (with text):**
```json
{
  "previewMode": "PDF",
  "previewSupported": true,
  "previewUrl": "https://...",
  "textContent": "...",
  "aiSupported": true,
  "message": "Xem PDF hoặc đọc text bên dưới"
}
```

**Success (scan/no text):**
```json
{
  "previewMode": "PDF",
  "previewSupported": true,
  "previewUrl": "https://...",
  "textContent": null,
  "aiSupported": false,
  "message": "Xem PDF trực tiếp. Không có text"
}
```

**Error (no permission):**
```json
{
  "code": 1003,
  "message": "Không có quyền truy cập"
}
```

---

### Files

| File | Purpose |
|------|---------|
| `PDF_PREVIEW_COMPLETE_GUIDE.md` | Full implementation (800+ lines) |
| `PDF_Preview_Test.postman_collection.json` | Test collection |
| `PDF_PREVIEW_FINAL_SUMMARY.md` | Detailed summary |
| `PDF_PREVIEW_QUICK_REFERENCE.md` | This card |

---

### Checklist

**Backend:** ✅ Done  
**Frontend:** ⏳ Use code in COMPLETE_GUIDE  
**Test:** ⏳ Use Postman collection  
**Deploy:** ⏳ After testing

---

**Build:** ✅ SUCCESS  
**Ready:** ✅ YES  
**Docs:** ✅ Complete
