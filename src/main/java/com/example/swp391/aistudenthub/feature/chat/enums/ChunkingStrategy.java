package com.example.swp391.aistudenthub.feature.chat.enums;

/**
 * Chiến lược để chia văn bản thành chunks.
 */
public enum ChunkingStrategy {
    /**
     * Chia theo số tokens cố định (800 tokens, overlap 150).
     * Phù hợp cho hầu hết trường hợp.
     */
    FIXED_SIZE,
    
    /**
     * Chia theo đoạn văn (paragraph).
     * Phù hợp cho tài liệu có cấu trúc rõ ràng.
     */
    PARAGRAPH,
    
    /**
     * Chia theo câu (sentence).
     * Phù hợp cho tài liệu ngắn hoặc Q&A pairs.
     */
    SENTENCE
}
