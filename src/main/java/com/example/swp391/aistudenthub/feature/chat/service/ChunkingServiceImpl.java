package com.example.swp391.aistudenthub.feature.chat.service;

import com.example.swp391.aistudenthub.feature.chat.dto.TextChunk;
import com.example.swp391.aistudenthub.feature.chat.enums.ChunkingStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChunkingServiceImpl implements ChunkingService {
    
    private static final int CHUNK_SIZE_TOKENS = 800;
    private static final int OVERLAP_TOKENS = 150;
    private static final int MIN_CHUNK_SIZE = 50;
    
    private static final Pattern SENTENCE_PATTERN = 
        Pattern.compile("(?<=[.!?])\\s+(?=[A-ZĐÀÁẢÃẠÂẦẤẨẪẬĂẰẮẲẴẶÈÉẺẼẸÊỀẾỂỄỆÌÍỈĨỊÒÓỎÕỌÔỒỐỔỖỘƠỜỚỞỠỢÙÚỦŨỤƯỪỨỬỮỰỲÝỶỸỴ])");
    
    private static final Pattern PARAGRAPH_PATTERN = 
        Pattern.compile("\\n\\s*\\n");
    
    @Override
    public List<TextChunk> chunkText(String text, ChunkingStrategy strategy) {
        if (!StringUtils.hasText(text)) {
            log.warn("Empty text provided for chunking");
            return List.of();
        }
        
        log.info("Chunking text of length {} using strategy {}", text.length(), strategy);
        
        List<TextChunk> chunks = switch (strategy) {
            case FIXED_SIZE -> chunkByFixedSize(text);
            case PARAGRAPH -> chunkByParagraph(text);
            case SENTENCE -> chunkBySentence(text);
        };
        
        log.info("Created {} chunks", chunks.size());
        return chunks;
    }
    
    private List<TextChunk> chunkByFixedSize(String text) {
        String[] words = text.split("\\s+");
        List<TextChunk> chunks = new ArrayList<>();
        int chunkIndex = 0;
        
        int start = 0;
        while (start < words.length) {
            int end = Math.min(start + CHUNK_SIZE_TOKENS, words.length);
            
            if (end < words.length) {
                end = findSentenceBoundary(words, end);
            }
            
            String[] chunkWords = Arrays.copyOfRange(words, start, end);
            String chunkContent = String.join(" ", chunkWords);
            
            if (chunkWords.length >= MIN_CHUNK_SIZE || start == 0 || end == words.length) {
                TextChunk chunk = TextChunk.builder()
                        .content(chunkContent)
                        .startPosition(start)
                        .endPosition(end)
                        .tokenCount(chunkWords.length)
                        .chunkIndex(chunkIndex++)
                        .build();
                chunks.add(chunk);
            }
            
            start = Math.max(start + 1, end - OVERLAP_TOKENS);
        }
        
        return chunks;
    }
    
    private int findSentenceBoundary(String[] words, int position) {
        for (int i = position; i < Math.min(position + 50, words.length); i++) {
            if (words[i].matches(".*[.!?]$")) {
                return i + 1;
            }
        }
        
        for (int i = position - 1; i > Math.max(position - 50, 0); i--) {
            if (words[i].matches(".*[.!?]$")) {
                return i + 1;
            }
        }
        
        return position;
    }
    
    private List<TextChunk> chunkByParagraph(String text) {
        String[] paragraphs = PARAGRAPH_PATTERN.split(text);
        List<TextChunk> chunks = new ArrayList<>();
        
        int wordPosition = 0;
        int chunkIndex = 0;
        
        for (String paragraph : paragraphs) {
            paragraph = paragraph.trim();
            if (!paragraph.isEmpty()) {
                String[] words = paragraph.split("\\s+");
                int tokenCount = words.length;
                
                if (tokenCount > CHUNK_SIZE_TOKENS) {
                    List<TextChunk> subChunks = chunkLongParagraph(
                        paragraph, wordPosition, chunkIndex
                    );
                    chunks.addAll(subChunks);
                    chunkIndex += subChunks.size();
                } else if (tokenCount >= MIN_CHUNK_SIZE) {
                    TextChunk chunk = TextChunk.builder()
                            .content(paragraph)
                            .startPosition(wordPosition)
                            .endPosition(wordPosition + tokenCount)
                            .tokenCount(tokenCount)
                            .chunkIndex(chunkIndex++)
                            .build();
                    chunks.add(chunk);
                }
                
                wordPosition += tokenCount;
            }
        }
        
        return chunks;
    }
    
    private List<TextChunk> chunkLongParagraph(String paragraph, int startPosition, int startIndex) {
        String[] words = paragraph.split("\\s+");
        List<TextChunk> chunks = new ArrayList<>();
        
        int start = 0;
        int chunkIndex = startIndex;
        
        while (start < words.length) {
            int end = Math.min(start + CHUNK_SIZE_TOKENS, words.length);
            String[] chunkWords = Arrays.copyOfRange(words, start, end);
            String chunkContent = String.join(" ", chunkWords);
            
            TextChunk chunk = TextChunk.builder()
                    .content(chunkContent)
                    .startPosition(startPosition + start)
                    .endPosition(startPosition + end)
                    .tokenCount(chunkWords.length)
                    .chunkIndex(chunkIndex++)
                    .build();
            chunks.add(chunk);
            
            start = end - OVERLAP_TOKENS;
        }
        
        return chunks;
    }
    
    private List<TextChunk> chunkBySentence(String text) {
        String[] sentences = SENTENCE_PATTERN.split(text);
        List<TextChunk> chunks = new ArrayList<>();
        
        int wordPosition = 0;
        int chunkIndex = 0;
        StringBuilder currentChunk = new StringBuilder();
        int currentTokenCount = 0;
        int chunkStartPosition = 0;
        
        for (String sentence : sentences) {
            sentence = sentence.trim();
            if (sentence.isEmpty()) continue;
            
            String[] words = sentence.split("\\s+");
            int sentenceTokens = words.length;
            
            if (currentTokenCount + sentenceTokens > CHUNK_SIZE_TOKENS && currentTokenCount > 0) {
                TextChunk chunk = TextChunk.builder()
                        .content(currentChunk.toString().trim())
                        .startPosition(chunkStartPosition)
                        .endPosition(wordPosition)
                        .tokenCount(currentTokenCount)
                        .chunkIndex(chunkIndex++)
                        .build();
                chunks.add(chunk);
                
                currentChunk = new StringBuilder();
                currentTokenCount = 0;
                chunkStartPosition = wordPosition;
            }
            
            if (currentChunk.length() > 0) {
                currentChunk.append(" ");
            }
            currentChunk.append(sentence);
            currentTokenCount += sentenceTokens;
            wordPosition += sentenceTokens;
        }
        
        if (currentTokenCount >= MIN_CHUNK_SIZE) {
            TextChunk chunk = TextChunk.builder()
                    .content(currentChunk.toString().trim())
                    .startPosition(chunkStartPosition)
                    .endPosition(wordPosition)
                    .tokenCount(currentTokenCount)
                    .chunkIndex(chunkIndex)
                    .build();
            chunks.add(chunk);
        }
        
        return chunks;
    }
}
