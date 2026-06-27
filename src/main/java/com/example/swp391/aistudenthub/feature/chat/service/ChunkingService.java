package com.example.swp391.aistudenthub.feature.chat.service;

import com.example.swp391.aistudenthub.feature.chat.dto.TextChunk;
import com.example.swp391.aistudenthub.feature.chat.enums.ChunkingStrategy;

import java.util.List;

/**
 * Service để chia văn bản thành các đoạn (chunks) nhỏ hơn.
 * Phục vụ cho RAG system - mỗi chunk sẽ được embed và lưu vào vector store.
 */
public interface ChunkingService {
    
    /**
     * Chia văn bản thành các chunks theo strategy.
     *
     * @param text Văn bản cần chia
     * @param strategy Chiến lược chunking
     * @return List các text chunks
     */
    List<TextChunk> chunkText(String text, ChunkingStrategy strategy);
    
    /**
     * Chia văn bản với strategy mặc định (FIXED_SIZE).
     *
     * @param text Văn bản cần chia
     * @return List các text chunks
     */
    default List<TextChunk> chunkText(String text) {
        return chunkText(text, ChunkingStrategy.FIXED_SIZE);
    }
}
