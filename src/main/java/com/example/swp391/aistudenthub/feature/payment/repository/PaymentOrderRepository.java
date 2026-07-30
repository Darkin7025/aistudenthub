package com.example.swp391.aistudenthub.feature.payment.repository;

import com.example.swp391.aistudenthub.feature.payment.entity.PaymentOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, UUID> {

    Optional<PaymentOrder> findByOrderCode(Long orderCode);

    List<PaymentOrder> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<PaymentOrder> findFirstByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, com.example.swp391.aistudenthub.feature.payment.enums.PaymentStatus status);

    boolean existsByUserIdAndStatus(UUID userId, com.example.swp391.aistudenthub.feature.payment.enums.PaymentStatus status);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(p.amount), 0) FROM PaymentOrder p WHERE p.status = 'PAID'")
    long calculateTotalRevenue();

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(p.amount), 0) FROM PaymentOrder p WHERE p.status = 'PAID' AND p.paidAt >= :startDate")
    long calculateRevenueFromDate(@org.springframework.data.repository.query.Param("startDate") java.time.OffsetDateTime startDate);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(DISTINCT p.userId) FROM PaymentOrder p WHERE p.status = 'PAID' AND p.paidAt >= :activeSince")
    long countActivePremiumUsers(@org.springframework.data.repository.query.Param("activeSince") java.time.OffsetDateTime activeSince);

    @org.springframework.data.jpa.repository.Query("SELECT p.description FROM PaymentOrder p WHERE p.status = 'PAID' GROUP BY p.description ORDER BY COUNT(p.id) DESC")
    List<String> findMostPopularPackages();

    List<PaymentOrder> findByStatusAndPaidAtAfterOrderByPaidAtAsc(com.example.swp391.aistudenthub.feature.payment.enums.PaymentStatus status, java.time.OffsetDateTime paidAt);
}
