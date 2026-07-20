package com.example.swp391.aistudenthub.feature.payment.dto.response;

import com.example.swp391.aistudenthub.feature.payment.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private UUID id;
    private Long orderCode;
    private UUID userId;
    private Integer amount;
    private String description;
    private PaymentStatus status;
    private String checkoutUrl;
    private String qrCode;
    private OffsetDateTime createdAt;
    private OffsetDateTime paidAt;
}
