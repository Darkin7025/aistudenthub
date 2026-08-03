package com.example.swp391.aistudenthub.feature.chat.repository;

import com.example.swp391.aistudenthub.feature.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.time.OffsetDateTime;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
        List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(UUID sessionId);

        long countBySessionId(UUID sessionId);

        @Query("SELECT COUNT(m.id) FROM ChatMessage m " +
                        "WHERE m.session.userId = :userId " +
                        "AND m.sender = com.example.swp391.aistudenthub.feature.chat.enums.MessageSender.USER " +
                        "AND m.createdAt >= :from")
        long countUserQuestionsSince(@Param("userId") UUID userId,
                                     @Param("from") OffsetDateTime from);

        @Query("SELECT m.session.id, COUNT(m.id) FROM ChatMessage m " +
                        "WHERE m.session.id IN :sessionIds GROUP BY m.session.id")
        List<Object[]> countBySessionIds(@Param("sessionIds") Collection<UUID> sessionIds);

        void deleteBySessionId(UUID sessionId);
}
