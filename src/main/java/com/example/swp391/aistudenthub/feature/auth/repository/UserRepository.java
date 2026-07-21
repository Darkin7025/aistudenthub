package com.example.swp391.aistudenthub.feature.auth.repository;

import com.example.swp391.aistudenthub.feature.auth.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    boolean existsByEmailAndDeletedAtIsNull(String email);

    Optional<User> findByIdAndDeletedAtIsNull(UUID id);

    // ---- Admin queries ----

    /** Tất cả users (kể cả đã xóa mềm), phân trang, sắp xếp theo createdAt DESC. */
    Page<User> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** Tìm kiếm user theo email hoặc fullName (không phân biệt hoa thường). */
    @Query("SELECT u FROM User u WHERE " +
           "(:keyword IS NULL OR LOWER(CAST(u.email AS string)) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) " +
           "OR LOWER(CAST(u.fullName AS string)) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))) " +
           "ORDER BY u.createdAt DESC")
    Page<User> searchUsers(@Param("keyword") String keyword, Pageable pageable);

    /** Số user chưa bị xóa mềm. */
    long countByDeletedAtIsNull();
}
