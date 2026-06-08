# AI Chatbot Fix Guide

## ✅ Đã Fix

Đã sửa bug **API format không tương thích** trong `DummyAIServiceImpl.java`:

### Vấn đề gốc:
- Code dùng **OpenAI API format** nhưng config trỏ đến **Gemini API**
- Dẫn đến API call fail → trả về mock response

### Thay đổi:
1. ✅ Tách riêng `callGeminiApi()` và `callOpenAiApi()` 
2. ✅ Implement đúng Gemini API format (v1beta generateContent)
3. ✅ Auto-detect provider và gọi đúng API
4. ✅ Cải thiện error logging để dễ debug
5. ✅ Tiếng Việt hóa error messages

## 🔍 Cách Test

### 1. Test với Mock Mode (không cần API key)
```properties
# application.properties
ai.provider=mock
```

Hoặc xóa/comment API key:
```properties
ai.api-key=
```

**Expected:** Trả về message giả lập bằng tiếng Việt

### 2. Test với Gemini API Real

#### Bước 1: Get Gemini API Key
1. Truy cập: https://aistudio.google.com/app/apikey
2. Click "Create API Key"
3. Copy API key (format: `AIza...`)

#### Bước 2: Config
```properties
# application.properties hoặc application-local.properties
ai.provider=gemini
ai.api-key=AIzaSy...YOUR_REAL_KEY_HERE
ai.api-url=https://generativelanguage.googleapis.com/v1beta/models
ai.model=gemini-1.5-flash
ai.max-tokens=1024
ai.timeout-seconds=30
```

Hoặc dùng environment variable:
```bash
export GEMINI_API_KEY=AIzaSy...YOUR_REAL_KEY_HERE
```

#### Bước 3: Test API
```bash
# Restart Spring Boot app
mvn spring-boot:run
```

Call API test:
```bash
curl -X POST http://localhost:8081/api/v1/chat \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Xin chào, bạn là ai?"
  }'
```

### 3. Test với OpenAI API (alternative)

```properties
ai.provider=openai
ai.api-key=sk-proj-...YOUR_OPENAI_KEY
ai.api-url=https://api.openai.com/v1/chat/completions
ai.model=gpt-4o-mini
ai.max-tokens=1024
ai.timeout-seconds=30
```

## ⚠️ Lưu Ý về API Key hiện tại

API key trong config:
```
YOUR_API_KEY_HERE
```

**Vấn đề:** Format này KHÔNG PHẢI Gemini API key hợp lệ.

- ✅ **Gemini API key format:** `AIza...` (bắt đầu bằng AIza)
- ❌ **Key hiện tại:** `AQ.Ab...` (không đúng format)

**Khuyến nghị:**
1. Generate API key mới từ Google AI Studio
2. Replace trong `application-local.properties` (KHÔNG commit vào Git)
3. Hoặc dùng environment variable `GEMINI_API_KEY`

## 📊 Log để Debug

Sau khi fix, logs sẽ rõ ràng hơn:

### Mock mode:
```
INFO  - AI service is running in mock mode. Provider: mock, API Key configured: false
```

### API call thành công:
```
DEBUG - Calling gemini API with model: gemini-1.5-flash
```

### API call thất bại:
```
ERROR - AI API request failed. Provider: gemini, Model: gemini-1.5-flash, Error: ...
ERROR - Gemini API error. Status: 400, Body: {"error": {"message": "..."}}
```

## 🎯 Next Steps

1. **Get valid Gemini API key** từ https://aistudio.google.com/app/apikey
2. **Config trong application-local.properties** (không commit)
3. **Restart app** và test lại
4. **Nếu vẫn lỗi:** Check logs để xem error message chi tiết

## 🔒 Security Note

**KHÔNG commit API key thật vào Git!**

- ✅ Dùng `application-local.properties` (đã có trong .gitignore)
- ✅ Hoặc dùng environment variables
- ❌ KHÔNG hardcode trong `application.properties`

## Support Multi-Provider

Code hiện tại đã hỗ trợ cả:
- ✅ **Gemini API** (Google)
- ✅ **OpenAI API** (ChatGPT)
- ✅ **Mock mode** (testing)

Chỉ cần đổi `ai.provider=gemini` hoặc `openai` hoặc `mock`.
