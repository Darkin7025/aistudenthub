# 🔧 Troubleshooting Guide - AI Student Hub

## 📄 PDF Preview Issues

### ❌ Problem: "Không có nội dung để xem trước"

**Possible Causes:**

1. **PDF là scan ảnh (không có text layer)**
   - ✅ Fix: Normal behavior - PDF vẫn xem được qua viewer
   - Message sẽ hiện: "File PDF không chứa văn bản (có thể là scan ảnh)"
   - Frontend: Hiển thị PDF viewer, không hiển thị AI chat button

2. **Text extraction failed**
   - Check logs: `ERROR ... Failed to extract text`
   - Có thể do PDF corrupt hoặc protected
   - ✅ Fix: PDF vẫn có thể xem được, chỉ không có AI support

3. **File bị lỗi upload**
   - Check: `fileUrl` có null không?
   - Check Cloudinary logs
   - ✅ Fix: Re-upload file

**Debug Steps:**
```bash
# 1. Check document record
curl -X GET "http://localhost:8081/api/v1/documents/{id}" \
  -H "Authorization: Bearer {JWT}"

# Look for:
# - fileUrl: should not be null
# - fileType: should be "application/pdf"

# 2. Check preview response
curl -X GET "http://localhost:8081/api/v1/documents/{id}/preview" \
  -H "Authorization: Bearer {JWT}"

# Look for:
# - previewMode: should be "PDF"
# - textContent: null = scanned PDF, has text = text-based PDF
# - aiSupported: true only if textContent exists
# - message: explains the situation

# 3. Check server logs
# Look for:
# "Extracted X chars from Y pages PDF: filename.pdf" (success)
# "PDF has no extractable text" (scanned PDF)
# "Failed to extract text" (error)
```

**Solutions:**

**If scanned PDF:**
```jsx
// Frontend: Show PDF viewer + info message
<PDFViewer url={previewUrl} />
<div className="info">
  📄 PDF này là ảnh scan. Xem trực tiếp bằng viewer trên.
</div>
```

**If text-based PDF:**
```jsx
// Frontend: Show PDF viewer + text + AI
<PDFViewer url={previewUrl} />
{textContent && <TextViewer content={textContent} />}
{aiSupported && <AIButton />}
```

---

## 🤖 AI Chatbot Issues

### ❌ Problem: AI trả về mock response

**Message:**
```
Đây là phản hồi AI giả lập. Hệ thống đang chạy ở chế độ mock...
```

**Cause:** Chưa config API key hoặc provider = "mock"

**Fix:**

**Step 1: Check config**
```bash
# Check application.properties
cat src/main/resources/application.properties | grep "ai\."

# Should see:
# ai.provider=gemini (or openai)
# ai.api-key=${GEMINI_API_KEY:...}
```

**Step 2: Create application-local.properties**
```properties
# Copy from example
cp src/main/resources/application-local.properties.example \
   src/main/resources/application-local.properties

# Edit
nano src/main/resources/application-local.properties

# Add valid API key:
ai.provider=gemini
ai.api-key=AIzaSy...YOUR_REAL_KEY_HERE
```

**Step 3: Get API Key**

**For Gemini (Free):**
1. Visit: https://aistudio.google.com/app/apikey
2. Click "Create API Key"
3. Copy key (starts with `AIza...`)
4. Paste into `application-local.properties`

**For OpenAI:**
1. Visit: https://platform.openai.com/api-keys
2. Create new key
3. Copy key (starts with `sk-proj-...`)
4. Update config:
   ```properties
   ai.provider=openai
   ai.api-key=sk-proj-...YOUR_KEY
   ai.api-url=https://api.openai.com/v1/chat/completions
   ai.model=gpt-4o-mini
   ```

**Step 4: Restart server**
```bash
# Stop current server (Ctrl+C)
# Start again
mvn spring-boot:run

# Or if running as JAR
java -jar target/aistudenthub-0.0.1-SNAPSHOT.jar
```

**Step 5: Test**
```bash
curl -X POST http://localhost:8081/api/v1/chat \
  -H "Authorization: Bearer {JWT}" \
  -H "Content-Type: application/json" \
  -d '{"message":"Hello, are you real AI now?"}'

# Should get real AI response, not mock
```

---

### ❌ Problem: "Dịch vụ AI tạm thời không khả dụng"

**Possible Causes:**

1. **API key invalid**
   - Error log: `401 Unauthorized` or `403 Forbidden`
   - ✅ Fix: Get new API key

2. **API quota exceeded**
   - Error log: `429 Too Many Requests`
   - ✅ Fix: Wait or upgrade plan

3. **Network issue**
   - Error log: `Connection timeout`
   - ✅ Fix: Check internet, firewall

4. **API endpoint wrong**
   - Error log: `404 Not Found`
   - ✅ Fix: Verify `ai.api-url`

**Debug Steps:**
```bash
# 1. Check server logs
# Look for detailed error:
# ERROR c.e.s.a.f.c.s.DummyAIServiceImpl - AI API request failed. Provider: gemini, Model: gemini-1.5-flash, Error: ...
# ERROR c.e.s.a.f.c.s.DummyAIServiceImpl - Gemini API error. Status: 401, Body: {"error": {...}}

# 2. Test API directly
# For Gemini:
curl "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=YOUR_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "contents": [{
      "parts": [{"text": "Test"}]
    }]
  }'

# For OpenAI:
curl https://api.openai.com/v1/chat/completions \
  -H "Authorization: Bearer YOUR_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gpt-4o-mini",
    "messages": [{"role": "user", "content": "Test"}]
  }'

# 3. Verify config loaded
# Add this to test endpoint or check startup logs:
# log.info("AI Config - Provider: {}, Model: {}, API URL: {}, Key configured: {}", 
#          provider, model, apiUrl, StringUtils.hasText(apiKey));
```

---

## 📤 Upload Issues

### ❌ Problem: File upload fails

**Error: "File too large"**
```
Cause: File > 10MB
Fix: Increase limit in DocumentService.java
```
```java
private static final long MAX_FILE_SIZE = 50L * 1024 * 1024; // 50MB
```

**Error: "Upload failed"**
```
Cause: Cloudinary connection issue
Check: Cloudinary credentials
```
```bash
# Test Cloudinary directly
curl -X POST "https://api.cloudinary.com/v1_1/YOUR_CLOUD_NAME/upload" \
  -F "file=@test.jpg" \
  -F "api_key=YOUR_API_KEY" \
  -F "timestamp=$(date +%s)" \
  -F "signature=YOUR_SIGNATURE"
```

**Error: "Empty file"**
```
Cause: Multipart request format wrong
Fix: Ensure Content-Type: multipart/form-data
```
```bash
# Correct format:
curl -X POST http://localhost:8081/api/v1/documents/upload \
  -H "Authorization: Bearer {JWT}" \
  -F "file=@document.pdf" \
  -F 'request={"title":"Test"};type=application/json'
```

---

## 🔐 Authentication Issues

### ❌ Problem: "401 Unauthorized"

**Cause 1: Missing token**
```bash
# ❌ Wrong
curl http://localhost:8081/api/v1/documents/my

# ✅ Correct
curl http://localhost:8081/api/v1/documents/my \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Cause 2: Expired token**
```
Fix: Login again to get new token
```
```bash
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123"
  }'
```

**Cause 3: Invalid token format**
```bash
# ❌ Wrong
Authorization: YOUR_JWT_TOKEN

# ✅ Correct
Authorization: Bearer YOUR_JWT_TOKEN
```

---

## 🗄️ Database Issues

### ❌ Problem: Connection refused

**Error:** `Connection refused: localhost:1444`

**Cause:** SQL Server not running

**Fix:**
```bash
# Check if SQL Server running
# Windows:
net start MSSQLSERVER

# Or check services
services.msc

# Verify connection
sqlcmd -S localhost,1444 -U sa -P your_password -Q "SELECT @@VERSION"
```

**Update connection string:**
```properties
# application-local.properties
spring.datasource.url=jdbc:sqlserver://localhost:1444;databaseName=ai_study_hub;trustServerCertificate=true;encrypt=false
spring.datasource.username=sa
spring.datasource.password=YOUR_PASSWORD
```

---

## 🌐 CORS Issues

### ❌ Problem: Frontend can't call API

**Error:** `CORS policy: No 'Access-Control-Allow-Origin' header`

**Fix:** Update `SecurityConfig.java`
```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(Arrays.asList(
        "http://localhost:5173", // Vite dev
        "http://localhost:3000"  // React dev
    ));
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(Arrays.asList("*"));
    configuration.setAllowCredentials(true);
    
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}
```

---

## 📊 Performance Issues

### ❌ Problem: Slow upload for large files

**Optimization:**

1. **Increase timeout**
```properties
spring.servlet.multipart.max-file-size=50MB
spring.servlet.multipart.max-request-size=50MB
server.tomcat.connection-timeout=60000
```

2. **Async processing**
```java
@Async
public CompletableFuture<DocumentResponse> uploadAsync(MultipartFile file) {
    // Upload in background
}
```

3. **Cloudinary optimization**
```java
Map<?, ?> result = cloudinary.uploader().upload(
    tempFile,
    ObjectUtils.asMap(
        "resource_type", "auto",
        "chunk_size", 6000000 // 6MB chunks
    )
);
```

---

## 🧪 Testing Issues

### ❌ Problem: Tests fail with 403

**Cause:** Missing authentication in tests

**Fix:**
```java
@Test
void testUpload() {
    // Get token first
    String token = loginAndGetToken("test@example.com", "password");
    
    // Then make authenticated request
    mockMvc.perform(multipart("/api/v1/documents/upload")
        .file("file", testFile.getBytes())
        .header("Authorization", "Bearer " + token))
        .andExpect(status().isCreated());
}
```

---

## 📝 Logging Tips

### Enable Debug Logging

```properties
# application.properties
logging.level.com.example.swp391.aistudenthub=DEBUG
logging.level.com.cloudinary=DEBUG
logging.level.org.apache.pdfbox=INFO
```

### Key Log Patterns

**Successful operations:**
```
INFO  - Document saved: id=..., user=...
INFO  - Extracted 5000 chars from 10 pages PDF: document.pdf
INFO  - Cloudinary upload success: publicId=...
DEBUG - Calling gemini API with model: gemini-1.5-flash
```

**Errors to watch:**
```
ERROR - AI API request failed. Provider: ..., Model: ..., Error: ...
ERROR - Failed to extract text from file ...
ERROR - Cloudinary upload failed
WARN  - PDF has no extractable text (might be scanned image)
```

---

## 🚑 Quick Fixes Checklist

- [ ] Server running? `http://localhost:8081/actuator/health`
- [ ] Database connected? Check logs for SQL Server errors
- [ ] JWT token valid? Test `/auth/login` first
- [ ] Cloudinary configured? Check env vars
- [ ] AI API key set? Check `application-local.properties`
- [ ] CORS enabled? Test from frontend origin
- [ ] File size OK? Max 10MB default
- [ ] Logs showing errors? Enable DEBUG level

---

## 📞 Getting Help

### Check Documentation
1. `IMPLEMENTATION_STATUS.md` - Feature overview
2. `FILE_PREVIEW_GUIDE.md` - Frontend integration
3. `PDF_PREVIEW_FIX.md` - PDF specific issues
4. `AI_FIX_GUIDE.md` - AI setup

### Check Logs Location
- Console output (if running via `mvn spring-boot:run`)
- `logs/application.log` (if configured)
- System logs (Windows Event Viewer, Linux syslog)

### Common Log Locations
```bash
# Application logs
tail -f logs/spring-boot-application.log

# Tomcat logs  
tail -f logs/catalina.out

# Check for errors
grep ERROR logs/*.log
```
