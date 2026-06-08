package com.example.swp391.aistudenthub.feature.chat.service;

import java.util.function.Consumer;

public interface AIService {
    String generateAnswer(String prompt);

    default String generateResponse(String prompt) {
        return generateAnswer(prompt);
    }

    void generateStreamResponse(String prompt, Consumer<String> onNext, Runnable onComplete, Consumer<Throwable> onError);
}
