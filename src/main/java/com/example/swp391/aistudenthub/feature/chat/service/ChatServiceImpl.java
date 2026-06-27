package com.example.swp391.aistudenthub.feature.chat.service;

import com.example.swp391.aistudenthub.exception.AppException;
import com.example.swp391.aistudenthub.exception.ErrorCode;
import com.example.swp391.aistudenthub.feature.chat.dto.request.ChatRequest;
import com.example.swp391.aistudenthub.feature.chat.dto.request.DocumentChatRequest;
import com.example.swp391.aistudenthub.feature.chat.dto.response.ChatMessageResponse;
import com.example.swp391.aistudenthub.feature.chat.dto.response.ChatResponse;
import com.example.swp391.aistudenthub.feature.chat.dto.response.ChatSessionResponse;
import com.example.swp391.aistudenthub.feature.chat.entity.ChatMessage;
import com.example.swp391.aistudenthub.feature.chat.entity.ChatSession;
import com.example.swp391.aistudenthub.feature.chat.enums.MessageSender;
import com.example.swp391.aistudenthub.feature.chat.mapper.ChatMapper;
import com.example.swp391.aistudenthub.feature.chat.repository.ChatMessageRepository;
import com.example.swp391.aistudenthub.feature.chat.repository.ChatSessionRepository;
import com.example.swp391.aistudenthub.feature.document.entity.Document;
import com.example.swp391.aistudenthub.feature.document.enums.PreviewMode;
import com.example.swp391.aistudenthub.feature.document.repository.DocumentRepository;
import com.example.swp391.aistudenthub.feature.document.service.DocumentPreviewResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private static final String AI_UNAVAILABLE_MESSAGE =
            "AI service is temporarily unavailable. Please try again later.";
    private static final int TITLE_MAX_LENGTH = 80;

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final DocumentRepository documentRepository;
    private final AIService aiService;
    private final RAGService ragService;
    private final ChatMapper chatMapper;
    private final TransactionTemplate transactionTemplate;
    private final DocumentPreviewResolver previewResolver;

    @Override
    public ChatResponse chat(ChatRequest request, UUID userId) {
        String message = requireText(request.getMessage());

        ChatSession session = transactionTemplate.execute(status -> {
            ChatSession currentSession = getOrCreateSession(
                    request.getSessionId(),
                    userId,
                    buildSessionTitle(message),
                    null
            );
            saveMessage(currentSession, MessageSender.USER, message);
            return currentSession;
        });
        if (session == null) {
            throw new AppException(ErrorCode.INTERNAL_ERROR);
        }

        String answer = generateAnswerSafely(message);
        UUID sessionId = session.getId();

        transactionTemplate.executeWithoutResult(status -> {
            ChatSession currentSession = findSessionForUser(sessionId, userId);
            saveMessage(currentSession, MessageSender.AI, answer);
        });

        return ChatResponse.builder()
                .answer(answer)
                .sessionId(sessionId)
                .documentId(session.getDocumentId())
                .build();
    }

    @Override
    public ChatResponse chatWithDocument(UUID documentId, DocumentChatRequest request, UUID userId) {
        String question = requireText(request.getQuestion());
        Document document = findOwnedDocument(documentId, userId);
        ensureDocumentChatCapable(document);

        ChatSession session = transactionTemplate.execute(status -> {
            ChatSession currentSession = getOrCreateSession(
                    request.getSessionId(),
                    userId,
                    "Hỏi về: " + document.getTitle(),
                    documentId
            );
            saveMessage(currentSession, MessageSender.USER, question);
            return currentSession;
        });
        if (session == null) {
            throw new AppException(ErrorCode.INTERNAL_ERROR);
        }

        String answer = generateAnswerForDocument(document, question);
        UUID sessionId = session.getId();

        transactionTemplate.executeWithoutResult(status -> {
            ChatSession currentSession = findSessionForUser(sessionId, userId);
            attachDocumentContext(currentSession, documentId);
            saveMessage(currentSession, MessageSender.AI, answer);
        });

        return ChatResponse.builder()
                .answer(answer)
                .sessionId(sessionId)
                .documentId(documentId)
                .build();
    }

    @Override
    public SseEmitter streamChat(ChatRequest request, UUID userId) {
        String message = requireText(request.getMessage());
        ChatSession session = transactionTemplate.execute(status -> {
            ChatSession currentSession = getOrCreateSession(
                    request.getSessionId(),
                    userId,
                    buildSessionTitle(message),
                    null
            );
            saveMessage(currentSession, MessageSender.USER, message);
            return currentSession;
        });
        if (session == null) {
            throw new AppException(ErrorCode.INTERNAL_ERROR);
        }

        return streamAnswer(message, session.getId(), userId, null);
    }

    @Override
    public SseEmitter streamChatWithDocument(UUID documentId, DocumentChatRequest request, UUID userId) {
        String question = requireText(request.getQuestion());
        Document document = findOwnedDocument(documentId, userId);
        ensureDocumentChatCapable(document);

        ChatSession session = transactionTemplate.execute(status -> {
            ChatSession currentSession = getOrCreateSession(
                    request.getSessionId(),
                    userId,
                    "Hỏi về: " + document.getTitle(),
                    documentId
            );
            saveMessage(currentSession, MessageSender.USER, question);
            return currentSession;
        });
        if (session == null) {
            throw new AppException(ErrorCode.INTERNAL_ERROR);
        }

        // Stream image via Vision: wrap answer as simulated stream
        PreviewMode mode = resolveDocumentMode(document);
        if (PreviewMode.IMAGE.equals(mode)) {
            String answer = generateImageAnswerSafely(document.getFileUrl(), question);
            UUID sessionId = session.getId();
            transactionTemplate.executeWithoutResult(status -> {
                ChatSession s = findSessionForUser(sessionId, userId);
                attachDocumentContext(s, documentId);
                saveMessage(s, MessageSender.AI, answer);
            });
            return simulateStream(answer, sessionId, userId, documentId);
        }

        String prompt = ragService.buildDocumentPrompt(document.getExtractedText(), question);
        return streamAnswer(prompt, session.getId(), userId, documentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatSessionResponse> getUserSessions(UUID userId) {
        return chatSessionRepository.findByUserIdOrderByUpdatedAtDesc(userId)
                .stream()
                .map(chatMapper::toSessionResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getSessionMessages(UUID sessionId, UUID userId) {
        ChatSession session = findSessionForUser(sessionId, userId);

        return chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId())
                .stream()
                .map(chatMapper::toMessageResponse)
                .toList();
    }

    @Override
    @Transactional
    public void deleteSession(UUID sessionId, UUID userId) {
        ChatSession session = findSessionForUser(sessionId, userId);
        chatMessageRepository.deleteAll(chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId()));
        chatSessionRepository.delete(session);
    }

    private SseEmitter streamAnswer(String prompt, UUID sessionId, UUID userId, UUID documentId) {
        SseEmitter emitter = new SseEmitter(60_000L);
        StringBuilder fullAnswer = new StringBuilder();

        aiService.generateStreamResponse(
                prompt,
                chunk -> {
                    try {
                        fullAnswer.append(chunk);
                        emitter.send(SseEmitter.event().name("message").data(chunk));
                    } catch (Exception e) {
                        emitter.completeWithError(e);
                    }
                },
                () -> {
                    try {
                        String answer = StringUtils.hasText(fullAnswer.toString())
                                ? fullAnswer.toString()
                                : AI_UNAVAILABLE_MESSAGE;
                        transactionTemplate.executeWithoutResult(status -> {
                            ChatSession session = findSessionForUser(sessionId, userId);
                            if (documentId != null) {
                                attachDocumentContext(session, documentId);
                            }
                            saveMessage(session, MessageSender.AI, answer);
                        });
                        emitter.send(SseEmitter.event().name("done").data(sessionId.toString()));
                        emitter.complete();
                    } catch (Exception e) {
                        emitter.completeWithError(e);
                    }
                },
                error -> {
                    log.warn("AI streaming failed: {}", error.getMessage());
                    try {
                        emitter.send(SseEmitter.event().name("error").data(AI_UNAVAILABLE_MESSAGE));
                    } catch (Exception ignored) {
                    }
                    emitter.complete();
                }
        );

        return emitter;
    }

    private ChatSession getOrCreateSession(UUID sessionId, UUID userId, String initialTitle, UUID documentId) {
        if (sessionId != null) {
            ChatSession session = findSessionForUser(sessionId, userId);
            if (documentId != null) {
                attachDocumentContext(session, documentId);
            }
            return session;
        }

        ChatSession newSession = ChatSession.builder()
                .userId(userId)
                .documentId(documentId)
                .title(limitTitle(initialTitle))
                .build();
        return chatSessionRepository.save(newSession);
    }

    private ChatSession findSessionForUser(UUID sessionId, UUID userId) {
        return chatSessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.CHAT_SESSION_NOT_FOUND));
    }

    private Document findOwnedDocument(UUID documentId, UUID userId) {
        Document document = documentRepository.findByIdAndDeletedAtIsNull(documentId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND));

        if (!document.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.FORBIDDEN_ACCESS);
        }

        return document;
    }

    private void attachDocumentContext(ChatSession session, UUID documentId) {
        if (session.getDocumentId() == null) {
            session.setDocumentId(documentId);
            return;
        }

        if (!session.getDocumentId().equals(documentId)) {
            throw new AppException(ErrorCode.CHAT_SESSION_DOCUMENT_MISMATCH);
        }
    }

    private void saveMessage(ChatSession session, MessageSender sender, String content) {
        ChatMessage message = ChatMessage.builder()
                .session(session)
                .sender(sender)
                .message(content)
                .build();
        chatMessageRepository.save(message);

        session.setUpdatedAt(OffsetDateTime.now());
        chatSessionRepository.save(session);
    }

    private String generateAnswerSafely(String prompt) {
        try {
            return aiService.generateAnswer(prompt);
        } catch (Exception e) {
            log.warn("AI service unavailable: {}", e.getMessage());
            return AI_UNAVAILABLE_MESSAGE;
        }
    }

    private String generateImageAnswerSafely(String imageUrl, String question) {
        try {
            return aiService.generateAnswerWithImage(imageUrl, question);
        } catch (Exception e) {
            log.warn("Gemini Vision unavailable: {}", e.getMessage());
            return AI_UNAVAILABLE_MESSAGE;
        }
    }

    /**
     * Generates the AI answer for a document, routing to Vision API for images
     * and RAG prompt for text-extractable documents.
     */
    private String generateAnswerForDocument(Document document, String question) {
        PreviewMode mode = resolveDocumentMode(document);
        if (PreviewMode.IMAGE.equals(mode)) {
            return generateImageAnswerSafely(document.getFileUrl(), question);
        }
        String prompt = ragService.buildDocumentPrompt(document.getExtractedText(), question);
        return generateAnswerSafely(prompt);
    }

    private PreviewMode resolveDocumentMode(Document document) {
        String fileName = document.getOriginalFileName() != null
                ? document.getOriginalFileName() : document.getFileName();
        return previewResolver.resolveMode(fileName, document.getFileType());
    }

    /**
     * Simulates a stream for Vision API responses (which are not natively streamed).
     */
    private SseEmitter simulateStream(String answer, UUID sessionId, UUID userId, UUID documentId) {
        SseEmitter emitter = new SseEmitter(60_000L);
        new Thread(() -> {
            try {
                String[] words = answer.split("(?<=\\s)");
                for (String word : words) {
                    Thread.sleep(60);
                    emitter.send(SseEmitter.event().name("message").data(word));
                }
                emitter.send(SseEmitter.event().name("done").data(sessionId.toString()));
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        }).start();
        return emitter;
    }

    private String requireText(String value) {
        if (!StringUtils.hasText(value)) {
            throw new AppException(ErrorCode.VALIDATION_ERROR);
        }
        return value.trim();
    }

    private String buildSessionTitle(String message) {
        return limitTitle(message);
    }

    private String limitTitle(String title) {
        if (!StringUtils.hasText(title)) {
            return "Hội thoại mới";
        }

        String normalized = title.trim().replaceAll("\\s+", " ");
        if (normalized.length() <= TITLE_MAX_LENGTH) {
            return normalized;
        }

        return normalized.substring(0, TITLE_MAX_LENGTH - 3) + "...";
    }

    /**
     * Ensures the document can be used for AI chat:
     * - Images: must have a fileUrl (for Vision API)
     * - Other types: must have extractedText (for RAG)
     */
    private void ensureDocumentChatCapable(Document document) {
        PreviewMode mode = resolveDocumentMode(document);
        if (PreviewMode.IMAGE.equals(mode)) {
            if (!StringUtils.hasText(document.getFileUrl())) {
                throw new AppException(ErrorCode.DOCUMENT_CONTENT_NOT_AVAILABLE,
                        "Hình ảnh không có URL để phân tích.");
            }
            return;
        }
        if (!StringUtils.hasText(document.getExtractedText())) {
            throw new AppException(ErrorCode.DOCUMENT_CONTENT_NOT_AVAILABLE);
        }
    }
}
