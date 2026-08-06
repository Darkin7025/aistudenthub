package com.example.swp391.aistudenthub.feature.chat.service;

import com.example.swp391.aistudenthub.exception.AppException;
import com.example.swp391.aistudenthub.exception.ErrorCode;
import com.example.swp391.aistudenthub.feature.chat.dto.request.ChatRequest;
import com.example.swp391.aistudenthub.feature.chat.dto.request.DocumentChatRequest;
import com.example.swp391.aistudenthub.feature.chat.dto.response.ChatMessageResponse;
import com.example.swp391.aistudenthub.feature.chat.dto.response.ChatResponse;
import com.example.swp391.aistudenthub.feature.chat.dto.response.DocumentChatQuotaResponse;
import com.example.swp391.aistudenthub.feature.chat.dto.response.ChatSessionResponse;
import com.example.swp391.aistudenthub.feature.chat.entity.ChatMessage;
import com.example.swp391.aistudenthub.feature.chat.entity.ChatSession;
import com.example.swp391.aistudenthub.feature.chat.enums.MessageSender;
import com.example.swp391.aistudenthub.feature.chat.mapper.ChatMapper;
import com.example.swp391.aistudenthub.feature.chat.repository.ChatMessageRepository;
import com.example.swp391.aistudenthub.feature.chat.repository.ChatSessionRepository;
import com.example.swp391.aistudenthub.feature.admin.repository.SystemConfigRepository;
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

import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import com.example.swp391.aistudenthub.feature.payment.repository.PaymentOrderRepository;
import com.example.swp391.aistudenthub.feature.payment.entity.PaymentOrder;
import com.example.swp391.aistudenthub.feature.payment.enums.PaymentStatus;

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
    private final SystemConfigRepository systemConfigRepository;
    private final com.example.swp391.aistudenthub.feature.chat.repository.DocumentChunkRepository documentChunkRepository;
    private final com.example.swp391.aistudenthub.feature.auth.repository.UserRepository userRepository;
    private final com.example.swp391.aistudenthub.feature.document.repository.DocumentShareRepository documentShareRepository;
    private final PaymentOrderRepository paymentOrderRepository;
    private final AiQuotaService aiQuotaService;

    @Override
    public ChatResponse chat(ChatRequest request, UUID userId) {
        checkAiChatFeatureEnabled();
        String message = requireText(request.getMessage());

        ChatSession session = transactionTemplate.execute(status -> {
            ChatSession currentSession = getOrCreateSession(
                    request.getSessionId(),
                    userId,
                    buildSessionTitle(message),
                    null
            );
            aiQuotaService.reserveQuestion(userId);
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
        checkAiChatFeatureEnabled();
        String question = requireText(request.getQuestion());
        Document document = findAccessibleDocument(documentId, userId);
        ensureDocumentChatCapable(document);
        checkDocumentChatLimit(document, userId);

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
        checkAiChatFeatureEnabled();
        String message = requireText(request.getMessage());
        ChatSession session = transactionTemplate.execute(status -> {
            ChatSession currentSession = getOrCreateSession(
                    request.getSessionId(),
                    userId,
                    buildSessionTitle(message),
                    null
            );
            aiQuotaService.reserveQuestion(userId);
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
        checkAiChatFeatureEnabled();
        String question = requireText(request.getQuestion());
        Document document = findAccessibleDocument(documentId, userId);
        ensureDocumentChatCapable(document);
        checkDocumentChatLimit(document, userId);

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
        if (document.getExtractedText() == null) {
            throw new AppException(ErrorCode.DOCUMENT_CONTENT_NOT_AVAILABLE);
        }
        String contextText = retrieveRelevantContext(documentId, question, document.getExtractedText());
        String prompt = ragService.buildDocumentPrompt(contextText, question);
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
            if (documentId == null && session.getDocumentId() != null) {
                throw new AppException(ErrorCode.CHAT_SESSION_MODE_MISMATCH);
            }
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

    private Document findAccessibleDocument(UUID documentId, UUID userId) {
        Document document = documentRepository.findByIdAndDeletedAtIsNull(documentId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND));

        if (document.getUserId().equals(userId)) {
            return document;
        }

        com.example.swp391.aistudenthub.feature.auth.entity.User user = userRepository.findById(userId)
                .orElse(null);

        if (user != null && com.example.swp391.aistudenthub.feature.auth.entity.Role.ADMIN.equals(user.getRole())) {
            return document;
        }

        if (com.example.swp391.aistudenthub.feature.document.enums.DocumentVisibility.PUBLIC.equals(document.getVisibility())) {
            return document;
        }

        boolean isShared = documentShareRepository.existsByDocumentIdAndSharedWithUserId(documentId, userId);
        if (isShared) {
            return document;
        }

        throw new AppException(ErrorCode.FORBIDDEN_ACCESS);
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
        if (document.getExtractedText() == null) {
            throw new AppException(ErrorCode.DOCUMENT_CONTENT_NOT_AVAILABLE);
        }
        String contextText = retrieveRelevantContext(document.getId(), question, document.getExtractedText());
        String prompt = ragService.buildDocumentPrompt(contextText, question);
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

    private void checkAiChatFeatureEnabled() {
        systemConfigRepository.findById("feature.ai_chat.enabled")
                .ifPresent(config -> {
                    if (!Boolean.parseBoolean(config.getConfigValue().trim())) {
                        throw new AppException(ErrorCode.FEATURE_DISABLED);
                    }
                });
    }

    // ---- Document Chat Limit helpers ----

    private record DocumentChatLimitInfo(int limit, String tierName, boolean isOwner) {}

    private DocumentChatLimitInfo resolveDocumentChatLimit(Document document, UUID userId) {
        boolean isOwner = document.getUserId().equals(userId);
        if (isOwner) {
            Optional<PaymentOrder> latestPaidOrder = paymentOrderRepository
                    .findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, PaymentStatus.PAID);

            int limit = 5;
            String tierName = "Cơ bản";

            if (latestPaidOrder.isPresent()) {
                int amount = latestPaidOrder.get().getAmount();
                if (amount >= 79000) {
                    limit = 15;
                    tierName = "Chuyên gia";
                } else if (amount >= 39000) {
                    limit = 10;
                    tierName = "Nâng cao";
                }
            }
            return new DocumentChatLimitInfo(limit, tierName, true);
        } else {
            return new DocumentChatLimitInfo(3, "Chia sẻ / Công khai", false);
        }
    }

    private void checkDocumentChatLimit(Document document, UUID userId) {
        long currentCount = chatMessageRepository.countByUserIdAndDocumentIdAndSender(userId, document.getId(), MessageSender.USER);
        DocumentChatLimitInfo info = resolveDocumentChatLimit(document, userId);

        if (currentCount >= info.limit()) {
            if (info.isOwner()) {
                throw new AppException(ErrorCode.LIMIT_EXCEEDED,
                        String.format("Bạn đang sử dụng gói %s. Giới hạn hỏi AI là %d lần cho tài liệu này.", info.tierName(), info.limit()));
            } else {
                throw new AppException(ErrorCode.LIMIT_EXCEEDED,
                        "Tài liệu chia sẻ / công khai chỉ cho phép hỏi AI tối đa 3 lần.");
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentChatQuotaResponse getDocumentChatQuota(UUID documentId, UUID userId) {
        Document document = findAccessibleDocument(documentId, userId);
        DocumentChatLimitInfo info = resolveDocumentChatLimit(document, userId);
        long used = chatMessageRepository.countByUserIdAndDocumentIdAndSender(userId, documentId, MessageSender.USER);

        return DocumentChatQuotaResponse.builder()
                .documentId(documentId)
                .documentTitle(document.getTitle())
                .isOwner(info.isOwner())
                .tierName(info.tierName())
                .limit(info.limit())
                .used(used)
                .remaining(Math.max(0, info.limit() - used))
                .build();
    }

    @Override
    @Transactional
    public ChatSessionResponse initDocumentChatSession(UUID documentId, UUID userId) {
        Document document = findAccessibleDocument(documentId, userId);
        ensureDocumentChatCapable(document);

        // Tìm session đã tồn tại cho cặp user + document
        Optional<ChatSession> existing = chatSessionRepository
                .findFirstByUserIdAndDocumentIdOrderByUpdatedAtDesc(userId, documentId);

        ChatSession session;
        if (existing.isPresent()) {
            session = existing.get();
        } else {
            session = ChatSession.builder()
                    .userId(userId)
                    .documentId(documentId)
                    .title("Hỏi về: " + document.getTitle())
                    .build();
            session = chatSessionRepository.save(session);
        }

        return chatMapper.toSessionResponse(session);
    }

    private String retrieveRelevantContext(UUID documentId, String question, String fallbackText) {
        try {
            java.util.List<com.example.swp391.aistudenthub.feature.chat.entity.DocumentChunk> chunks = 
                    documentChunkRepository.findByDocumentIdOrderByChunkIndexAsc(documentId);
            if (chunks == null || chunks.isEmpty()) {
                return fallbackText;
            }
            
            List<String> keywords = tokenizeForSearch(question);
            java.util.List<com.example.swp391.aistudenthub.feature.chat.entity.DocumentChunk> scored = new java.util.ArrayList<>(chunks);
            scored.sort((c1, c2) -> {
                long score1 = countMatches(c1.getContent(), keywords);
                long score2 = countMatches(c2.getContent(), keywords);
                if (score1 == score2) {
                    return Integer.compare(c1.getChunkIndex(), c2.getChunkIndex());
                }
                return Long.compare(score2, score1); // Descending order of relevance
            });

            if (!keywords.isEmpty() && countMatches(scored.get(0).getContent(), keywords) == 0) {
                return "[No relevant content was found in the uploaded document.]";
            }
            
            // Collect the top 3 chunks (approx ~2400 words max)
            StringBuilder sb = new StringBuilder();
            int topCount = Math.min(3, scored.size());
            for (int i = 0; i < topCount; i++) {
                sb.append(scored.get(i).getContent()).append("\n\n");
            }
            return sb.toString().trim();
        } catch (Exception e) {
            log.error("Failed to retrieve relevant context from chunks for document: {}", documentId, e);
            return fallbackText;
        }
    }

    private long countMatches(String content, List<String> keywords) {
        long count = 0;
        String normalizedContent = normalizeForSearch(content);
        for (String kw : keywords) {
            if (normalizedContent.contains(kw)) {
                count++;
            }
        }
        return count;
    }

    private List<String> tokenizeForSearch(String value) {
        Set<String> stopWords = Set.of("and", "the", "for", "with", "what", "this", "that", "cua", "hoi", "ve", "la", "va", "cho", "mot", "nhung", "cac");
        return List.of(normalizeForSearch(value).split("\\s+"))
                .stream()
                .filter(term -> term.length() >= 3 && !stopWords.contains(term))
                .distinct()
                .toList();
    }

    private String normalizeForSearch(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replace("đ", "d")
                .replace("Đ", "D")
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        return normalized.replaceAll("[^\\p{L}\\p{Nd}]+", " ").trim();
    }
}
