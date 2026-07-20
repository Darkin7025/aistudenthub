package com.example.swp391.aistudenthub.feature.payment.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentRequest {

    @NotNull(message = "Số tiền không được để trống")
    @Min(value = 2000, message = "Số tiền tối thiểu phải là 2.000 VNĐ")
    private Integer amount;

    @NotBlank(message = "Nội dung thanh toán không được để trống")
    private String description;

    private String returnUrl;
    private String cancelUrl;
}
