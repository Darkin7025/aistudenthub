package com.example.swp391.aistudenthub.feature.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Đại diện cho một đoạn văn bản (chunk) được tách ra từ tài liệu.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TextChunk {
    
    /**
     * Nội dung text của chunk.
     */
    private String content;
    
    /**
     * Vị trí bắt đầu trong tài liệu gốc (theo word index).
     */
    private int startPosition;
    
    /**
     * Vị trí kết thúc trong tài liệu gốc (theo word index).
     */
    private int endPosition;
    
    /**
     * Số lượng tokens/words ước tính trong chunk.
     */
    private int tokenCount;
    
    /**
     * Index của chunk trong tài liệu (0-based).
     */
    private int chunkIndex;
}
