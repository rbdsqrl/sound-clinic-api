package com.simplehearing.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PaymentVerifyRequest(
    @NotNull(message = "Appointment ID is required")
    Long appointmentId,

    @NotBlank(message = "Razorpay order ID is required")
    String razorpayOrderId,

    @NotBlank(message = "Razorpay payment ID is required")
    String razorpayPaymentId,

    @NotBlank(message = "Razorpay signature is required")
    String razorpaySignature
) {}
