# AIStudentHUB: document AI flow

## 1. Searching a user's own documents

`GET /api/v1/documents?keyword=...` searches only documents owned by the authenticated user. The keyword is matched against the title, original filename, description, subject, major, document type and extracted text. `folderId`, `subject` and `major` remain available as filters.

The query is always scoped by `userId` in the repository, so one user cannot search another user's private documents.

## 2. Two chat modes

The backend exposes two separate operations:

- `POST /api/v1/chat`: general AI chat. It does not receive document context.
- `POST /api/v1/chat/document/{documentId}`: document chat. The document must belong to the current user and must have extractable content (or be an image supported by Vision).

A document chat session cannot be reused through general chat. This rule is checked in the service, so it cannot be bypassed by changing the frontend request.

For document chat, the prompt instructs Gemini to use only the supplied document context. If no relevant chunk is found, the context contains a clear no-match marker and the model is instructed to say that the document does not contain enough information instead of answering from general knowledge.

## 3. How the document becomes AI knowledge

This project uses RAG (Retrieval-Augmented Generation), not fine-tuning:

1. The uploaded file is stored in Cloudinary.
2. `DocumentProcessor` downloads the file from the storage URL and extracts text from TXT, PDF and supported Office files. Images are sent to Gemini Vision when the user asks about them.
3. Extracted text is limited to 500 KB and split into chunks of about 800 words with 150 words of overlap.
4. Chunks are stored in `document_chunks` with their document ID and position.
5. At question time, the backend normalizes Vietnamese/English terms, ranks chunks by term overlap and sends the best three chunks to Gemini.
6. Gemini generates the answer from that context. The original model is not retrained and its global knowledge is not changed.

When document content is edited through the API, old chunks are deleted and rebuilt so the AI does not answer from stale content.

## 4. AI question limit

The default limit is **20 questions per account per day**. The counter is based on user messages, not Gemini output tokens. It is reset at the next local calendar day.

- `GET /api/v1/chat/quota` returns `dailyLimit`, `used`, `remaining` and `resetAt`.
- Admin can update `system.ai_daily_question_limit` through the system-config API. Valid values are 1 to 10,000.
- `ai.max-tokens` is a separate Gemini response-size limit. It controls the maximum generated output for one request and is not the daily question quota.
