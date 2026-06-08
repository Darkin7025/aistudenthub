package com.example.swp391.aistudenthub.feature.chat.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RAGService {

    private static final int MAX_CONTEXT_LENGTH = 10000;

    public String buildDocumentPrompt(String documentText, String question) {
        String context = truncateContext(documentText);

        return String.format(
                "System:\n" +
                "Bạn là AI Study Assistant của AI Study Hub. Nhiệm vụ của bạn là trả lời câu hỏi học tập dựa trên tài liệu người dùng đã upload.\n\n" +
                "Instruction:\n" +
                "Chỉ sử dụng nội dung tài liệu bên dưới để trả lời câu hỏi.\n" +
                "Nếu tài liệu không chứa đủ thông tin, hãy nói rõ rằng tài liệu không có đủ thông tin.\n" +
                "Không được tự bịa thêm kiến thức ngoài tài liệu.\n" +
                "Trả lời bằng cùng ngôn ngữ với câu hỏi của người dùng.\n" +
                "Trình bày ngắn gọn, rõ ràng, dễ hiểu cho sinh viên.\n\n" +
                "Document content:\n" +
                "%s\n\n" +
                "User question:\n" +
                "%s\n\n" +
                "Answer:\n",
                context,
                question == null ? "" : question.trim()
        );
    }

    public String buildPromptWithContext(String extractedText, String question) {
        return buildDocumentPrompt(extractedText, question);
    }

    private String truncateContext(String documentText) {
        if (!StringUtils.hasText(documentText)) {
            return "";
        }

        String trimmedText = documentText.trim();
        if (trimmedText.length() <= MAX_CONTEXT_LENGTH) {
            return trimmedText;
        }

        // TODO: chunking/vector database
        return trimmedText.substring(0, MAX_CONTEXT_LENGTH)
                + "\n\n[Nội dung đã bị cắt vì quá dài]";
    }
}
