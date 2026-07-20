package com.example.swp391.aistudenthub.feature.payment.controller;

import com.example.swp391.aistudenthub.common.dto.ApiResponse;
import com.example.swp391.aistudenthub.feature.auth.entity.User;
import com.example.swp391.aistudenthub.feature.payment.dto.request.CreatePaymentRequest;
import com.example.swp391.aistudenthub.feature.payment.dto.response.PaymentResponse;
import com.example.swp391.aistudenthub.feature.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.payos.type.Webhook;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(
            @Valid @RequestBody CreatePaymentRequest request,
            @AuthenticationPrincipal User currentUser) {
        PaymentResponse response = paymentService.createPaymentLink(request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Tạo link thanh toán PayOS thành công"));
    }

    @PostMapping("/payos-webhook")
    public ResponseEntity<ApiResponse<String>> handleWebhook(@RequestBody Webhook webhook) {
        String result = paymentService.handlePayOSWebhook(webhook);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getMyPaymentOrders(
            @AuthenticationPrincipal User currentUser) {
        List<PaymentResponse> orders = paymentService.getMyPaymentOrders(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/{orderCode}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentOrder(
            @PathVariable Long orderCode,
            @AuthenticationPrincipal User currentUser) {
        PaymentResponse order = paymentService.getPaymentOrder(orderCode, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @PostMapping("/{orderCode}/cancel")
    public ResponseEntity<ApiResponse<PaymentResponse>> cancelPaymentOrder(
            @PathVariable Long orderCode,
            @AuthenticationPrincipal User currentUser) {
        PaymentResponse response = paymentService.cancelPaymentOrder(orderCode, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(response, "Hủy đơn hàng thanh toán thành công"));
    }
}
