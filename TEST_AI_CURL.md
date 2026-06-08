# Test AI Chatbot API

## 1. Test Gemini API trực tiếp (không qua Spring Boot)

Để verify API key có hoạt động không:

```bash
curl "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -X POST \
  -d '{
    "contents": [
      {
        "parts": [
          {
            "text": "Xin chào, bạn là ai?"
          }
        ]
      }
    ],
    "generationConfig": {
      "temperature": 0.2,
      "maxOutputTokens": 1024
    }
  }'
```

**Expected response:**
```json
{
  "candidates": [
    {
      "content": {
        "parts": [
          {
            "text": "Xin chào! Tôi là Gemini..."
          }
        ],
        "role": "model"
      },
      "finishReason": "STOP"
    }
  ]
}
```

## 2. Test qua Spring Boot Chat API

### Step 1: Login để lấy JWT token

```bash
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "your-email@example.com",
    "password": "your-password"
  }'
```

Response:
```json
{
  "data": {
    "accessToken": "eyJhbGc...",
    "refreshToken": "...",
    "tokenType": "Bearer"
  }
}
```

### Step 2: Chat với AI

```bash
curl -X POST http://localhost:8081/api/v1/chat \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Xin chào, bạn có thể giúp tôi học Java không?"
  }'
```

**Expected (mock mode):**
```json
{
  "data": {
    "message": "Đây là phản hồi AI giả lập. Hệ thống đang chạy ở chế độ mock...",
    "sessionId": "...",
    "timestamp": "..."
  }
}
```

**Expected (real API):**
```json
{
  "data": {
    "message": "Chắc chắn rồi! Tôi có thể giúp bạn học Java...",
    "sessionId": "...",
    "timestamp": "..."
  }
}
```

## 3. Test Chat với Document (RAG)

```bash
curl -X POST http://localhost:8081/api/v1/chat/document/DOCUMENT_UUID \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Tóm tắt nội dung chính của tài liệu này"
  }'
```

## 4. Test Chat History

```bash
# Lấy danh sách sessions
curl -X GET http://localhost:8081/api/v1/chat/sessions \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Lấy messages trong session
curl -X GET http://localhost:8081/api/v1/chat/sessions/SESSION_UUID/messages \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## 5. Check Logs

Xem Spring Boot console để debug:

### Mock mode log:
```
INFO  c.e.s.a.f.c.s.DummyAIServiceImpl - AI service is running in mock mode. Provider: mock, API Key configured: false
```

### Real API log (success):
```
DEBUG c.e.s.a.f.c.s.DummyAIServiceImpl - Calling gemini API with model: gemini-1.5-flash
```

### Real API log (fail):
```
ERROR c.e.s.a.f.c.s.DummyAIServiceImpl - AI API request failed. Provider: gemini, Model: gemini-1.5-flash, Error: ...
ERROR c.e.s.a.f.c.s.DummyAIServiceImpl - Gemini API error. Status: 400, Body: {...}
```

## Common Errors

### 1. "API key not valid"
- API key format sai hoặc expired
- Get new key từ https://aistudio.google.com/app/apikey

### 2. "Quota exceeded"
- Gemini free tier có giới hạn requests/minute
- Wait hoặc upgrade plan

### 3. "401 Unauthorized" (chat endpoint)
- JWT token missing hoặc invalid
- Re-login để lấy token mới

### 4. "Model not found"
- Model name sai
- Check available models: `gemini-1.5-flash`, `gemini-1.5-pro`

## PowerShell Version (Windows)

```powershell
# Test Gemini API directly
$headers = @{
    "Content-Type" = "application/json"
}
$body = @{
    contents = @(
        @{
            parts = @(
                @{
                    text = "Xin chào, bạn là ai?"
                }
            )
        }
    )
    generationConfig = @{
        temperature = 0.2
        maxOutputTokens = 1024
    }
} | ConvertTo-Json -Depth 10

Invoke-RestMethod -Uri "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=YOUR_API_KEY" -Method Post -Headers $headers -Body $body

# Test Spring Boot chat
$jwt = "YOUR_JWT_TOKEN"
$headers = @{
    "Authorization" = "Bearer $jwt"
    "Content-Type" = "application/json"
}
$body = @{
    message = "Xin chào AI"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8081/api/v1/chat" -Method Post -Headers $headers -Body $body
```
