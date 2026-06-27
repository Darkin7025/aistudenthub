package com.example.swp391.aistudenthub.feature.chat.service;

import java.util.function.Consumer;

public interface AIService {
    String generateAnswer(String prompt);

    default String generateResponse(String prompt) {
        return generateAnswer(prompt);
    }

    /**
     * Sends an image URL and a question to the AI (Gemini Vision).
     * Used when the document is an image file with no extracted text.
     */
    String generateAnswerWithImage(String imageUrl, String question);

    void generateStreamResponse(String prompt, Consumer<String> onNext, Runnable onComplete, Consumer<Throwable> onError);
}
