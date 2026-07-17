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
    boolean existsByUserIdAndTitle(UUID userId, String title);

    // ---- Admin / Dashboard ----
    /** Tổng số chat sessions (count() đã có sẵn từ JpaRepository). */
    org.springframework.data.domain.Page<ChatSession> findAllByOrderByUpdatedAtDesc(org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT c FROM ChatSession c, User u WHERE c.userId = u.id AND " +
           "(:keyword IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY c.updatedAt DESC")
    org.springframework.data.domain.Page<ChatSession> searchSessions(@org.springframework.data.repository.query.Param("keyword") String keyword, org.springframework.data.domain.Pageable pageable);
}
