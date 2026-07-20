package com.example.swp391.aistudenthub.feature.payment.service;

import com.example.swp391.aistudenthub.exception.AppException;
import com.example.swp391.aistudenthub.feature.payment.dto.request.CreatePaymentRequest;
import com.example.swp391.aistudenthub.feature.payment.dto.response.PaymentResponse;
import com.example.swp391.aistudenthub.feature.payment.entity.PaymentOrder;
import com.example.swp391.aistudenthub.feature.payment.enums.PaymentStatus;
import com.example.swp391.aistudenthub.feature.payment.repository.PaymentOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.payos.PayOS;
import vn.payos.type.CheckoutResponseData;
import vn.payos.type.PaymentData;
import vn.payos.type.Webhook;
import vn.payos.type.WebhookData;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PayOS payOS;

    @Mock
    private PaymentOrderRepository paymentOrderRepository;

    @InjectMocks
    private PaymentService paymentService;

    private UUID userId;
    private Long orderCode;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        orderCode = 123456789L;
    }

    @Test
    void createPaymentLink_Success() throws Exception {
        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .amount(50000)
                .description("Nạp tiền tài khoản AI Hub")
                .build();

        CheckoutResponseData checkoutData = CheckoutResponseData.builder()
                .bin("970415")
                .accountNumber("123456789")
                .accountName("ACCOUNT TEST")
                .amount(50000)
                .description("Nạp tiền tài khoản AI Hub")
                .orderCode(orderCode)
                .currency("VND")
                .paymentLinkId("link-123")
                .status("PENDING")
                .checkoutUrl("https://pay.payos.vn/web/123456789")
                .qrCode("000201010212...")
                .build();

        when(payOS.createPaymentLink(any(PaymentData.class))).thenReturn(checkoutData);
        when(paymentOrderRepository.save(any(PaymentOrder.class))).thenAnswer(i -> i.getArgument(0));

        PaymentResponse response = paymentService.createPaymentLink(request, userId);

        assertNotNull(response);
        assertEquals(50000, response.getAmount());
        assertEquals("Nạp tiền tài khoản AI Hub", response.getDescription());
        assertEquals(PaymentStatus.PENDING, response.getStatus());
        assertEquals("https://pay.payos.vn/web/123456789", response.getCheckoutUrl());
        verify(paymentOrderRepository, times(1)).save(any(PaymentOrder.class));
    }

    @Test
    void handlePayOSWebhook_Success_MarksOrderAsPaid() throws Exception {
        PaymentOrder order = PaymentOrder.builder()
                .id(UUID.randomUUID())
                .orderCode(orderCode)
                .userId(userId)
                .amount(50000)
                .description("Thanh toan")
                .status(PaymentStatus.PENDING)
                .build();

        WebhookData webhookData = WebhookData.builder()
                .orderCode(orderCode)
                .amount(50000)
                .description("Thanh toan")
                .accountNumber("123456789")
                .reference("FT12345678")
                .transactionDateTime("2026-07-20 20:00:00")
                .currency("VND")
                .paymentLinkId("link-123")
                .code("00")
                .desc("Success")
                .build();

        Webhook webhook = Webhook.builder()
                .code("00")
                .desc("Success")
                .success(true)
                .signature("mock-signature")
                .data(webhookData)
                .build();

        when(payOS.verifyPaymentWebhookData(webhook)).thenReturn(webhookData);
        when(paymentOrderRepository.findByOrderCode(orderCode)).thenReturn(Optional.of(order));
        when(paymentOrderRepository.save(any(PaymentOrder.class))).thenAnswer(i -> i.getArgument(0));

        String result = paymentService.handlePayOSWebhook(webhook);

        assertEquals("Webhook processed successfully", result);
        assertEquals(PaymentStatus.PAID, order.getStatus());
        assertNotNull(order.getPaidAt());
        verify(paymentOrderRepository, times(1)).save(order);
    }

    @Test
    void getPaymentOrder_ThrowsForbidden_WhenNotOwner() {
        PaymentOrder order = PaymentOrder.builder()
                .id(UUID.randomUUID())
                .orderCode(orderCode)
                .userId(userId)
                .build();

        when(paymentOrderRepository.findByOrderCode(orderCode)).thenReturn(Optional.of(order));

        UUID strangerId = UUID.randomUUID();
        assertThrows(AppException.class, () -> paymentService.getPaymentOrder(orderCode, strangerId));
    }
}
