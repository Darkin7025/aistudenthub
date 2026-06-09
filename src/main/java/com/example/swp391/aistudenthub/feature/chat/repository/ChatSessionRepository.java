package com.example.swp391.aistudenthub.feature.chat.repository;

import com.example.swp391.aistudenthub.feature.chat.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {
    List<ChatSession> findByUserIdOrderByUpdatedAtDesc(UUID userId);
    Optional<ChatSession> findByIdAndUserId(UUID id, UUID userId);
}
