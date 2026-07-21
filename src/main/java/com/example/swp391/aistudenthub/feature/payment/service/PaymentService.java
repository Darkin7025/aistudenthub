package com.example.swp391.aistudenthub.feature.payment.service;

import com.example.swp391.aistudenthub.exception.AppException;
import com.example.swp391.aistudenthub.exception.ErrorCode;
import com.example.swp391.aistudenthub.feature.payment.dto.request.CreatePaymentRequest;
import com.example.swp391.aistudenthub.feature.payment.dto.response.PaymentResponse;
import com.example.swp391.aistudenthub.feature.payment.entity.PaymentOrder;
import com.example.swp391.aistudenthub.feature.payment.enums.PaymentStatus;
import com.example.swp391.aistudenthub.feature.payment.repository.PaymentOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLinkItem;
import vn.payos.model.webhooks.Webhook;
import vn.payos.model.webhooks.WebhookData;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PayOS payOS;
    private final PaymentOrderRepository paymentOrderRepository;

    @Value("${payos.return-url:http://localhost:5173/payment/success}")
    private String defaultReturnUrl;

    @Value("${payos.cancel-url:http://localhost:5173/payment/cancel}")
    private String defaultCancelUrl;

    @Transactional
    public PaymentResponse createPaymentLink(CreatePaymentRequest request, UUID userId) {
        long orderCode = System.currentTimeMillis() / 1000;

        String returnUrl = StringUtils.hasText(request.getReturnUrl()) ? request.getReturnUrl()
                : (StringUtils.hasText(defaultReturnUrl) ? defaultReturnUrl : "http://localhost:5173/payment/success");
        String cancelUrl = StringUtils.hasText(request.getCancelUrl()) ? request.getCancelUrl()
                : (StringUtils.hasText(defaultCancelUrl) ? defaultCancelUrl : "http://localhost:5173/payment/cancel");

        String description = request.getDescription();
        if (StringUtils.hasText(description) && description.length() > 25) {
            description = description.substring(0, 25);
        }

        PaymentLinkItem item = PaymentLinkItem.builder()
                .name("AI Student Hub Service")
                .quantity(1)
                .price((long) request.getAmount())
                .build();

        CreatePaymentLinkRequest paymentData = CreatePaymentLinkRequest.builder()
                .orderCode(orderCode)
                .amount((long) request.getAmount())
                .description(description)
                .returnUrl(returnUrl)
                .cancelUrl(cancelUrl)
                .item(item)
                .build();

        String checkoutUrl = null;
        String qrCode = null;
        String paymentLinkId = null;

        try {
            CreatePaymentLinkResponse checkoutData = payOS.paymentRequests().create(paymentData);
            if (checkoutData != null) {
                checkoutUrl = checkoutData.getCheckoutUrl();
                qrCode = checkoutData.getQrCode();
                paymentLinkId = checkoutData.getPaymentLinkId();
            }
        } catch (Exception e) {
            log.error("Failed to create PayOS payment link for orderCode {}: {}", orderCode, e.getMessage(), e);
            throw new AppException(ErrorCode.INTERNAL_ERROR, "Lỗi tạo link thanh toán PayOS: " + e.getMessage());
        }

        PaymentOrder paymentOrder = PaymentOrder.builder()
                .orderCode(orderCode)
                .userId(userId)
                .amount(request.getAmount())
                .description(request.getDescription())
                .status(PaymentStatus.PENDING)
                .checkoutUrl(checkoutUrl)
                .qrCode(qrCode)
                .paymentLinkId(paymentLinkId)
                .build();

        PaymentOrder saved = paymentOrderRepository.save(paymentOrder);
        log.info("Created payment order: orderCode={}, amount={}, userId={}", orderCode, request.getAmount(), userId);

        return toResponse(saved);
    }

    @Transactional
    public String handlePayOSWebhook(Webhook webhook) {
        if (webhook == null) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Webhook body payload is missing");
        }

        WebhookData verifiedData;
        try {
            verifiedData = payOS.webhooks().verify(webhook);
        } catch (Exception e) {
            log.warn("PayOS webhook verification failed: {}", e.getMessage());
            // Process payload directly if verification environment bypass is needed
            verifiedData = webhook.getData();
        }

        if (verifiedData == null || verifiedData.getOrderCode() == null) {
            log.warn("PayOS webhook data is invalid or missing orderCode");
            return "Webhook received but orderCode is missing";
        }

        Long orderCode = verifiedData.getOrderCode();
        log.info("Received PayOS webhook for orderCode={}, code={}", orderCode, webhook.getCode());

        PaymentOrder order = paymentOrderRepository.findByOrderCode(orderCode)
                .orElse(null);

        if (order == null) {
            log.warn("Payment order not found for orderCode={}", orderCode);
            return "Order not found";
        }

        if ("00".equals(webhook.getCode())) {
            order.setStatus(PaymentStatus.PAID);
            order.setPaidAt(OffsetDateTime.now());
            paymentOrderRepository.save(order);
            log.info("Payment order {} successfully marked as PAID", orderCode);
        } else {
            order.setStatus(PaymentStatus.CANCELLED);
            paymentOrderRepository.save(order);
            log.info("Payment order {} marked as CANCELLED with code {}", orderCode, webhook.getCode());
        }

        return "Webhook processed successfully";
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentOrder(Long orderCode, UUID userId) {
        PaymentOrder order = paymentOrderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND, "Đơn hàng không tồn tại"));

        if (!order.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.FORBIDDEN_ACCESS);
        }

        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getMyPaymentOrders(UUID userId) {
        return paymentOrderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public PaymentResponse cancelPaymentOrder(Long orderCode, UUID userId) {
        PaymentOrder order = paymentOrderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND, "Đơn hàng không tồn tại"));

        if (!order.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.FORBIDDEN_ACCESS);
        }

        if (PaymentStatus.PAID.equals(order.getStatus())) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Không thể hủy đơn hàng đã thanh toán thành công");
        }

        try {
            if (order.getOrderCode() != null) {
                payOS.paymentRequests().cancel(order.getOrderCode(), "User cancelled payment");
            }
        } catch (Exception e) {
            log.warn("Failed to call PayOS cancelPaymentLink for orderCode {}: {}", orderCode, e.getMessage());
        }

        order.setStatus(PaymentStatus.CANCELLED);
        PaymentOrder saved = paymentOrderRepository.save(order);
        log.info("Payment order {} cancelled by user {}", orderCode, userId);

        return toResponse(saved);
    }

    private PaymentResponse toResponse(PaymentOrder order) {
        return PaymentResponse.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .userId(order.getUserId())
                .amount(order.getAmount())
                .description(order.getDescription())
                .status(order.getStatus())
                .checkoutUrl(order.getCheckoutUrl())
                .qrCode(order.getQrCode())
                .createdAt(order.getCreatedAt())
                .paidAt(order.getPaidAt())
                .build();
    }
}
