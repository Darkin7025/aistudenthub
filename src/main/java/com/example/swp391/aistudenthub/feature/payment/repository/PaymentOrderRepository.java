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
}
