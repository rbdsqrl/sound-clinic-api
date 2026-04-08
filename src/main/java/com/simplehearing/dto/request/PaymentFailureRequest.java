package com.simplehearing.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PaymentFailureRequest(
    @NotNull(message = "Appointment ID is required")
    Long appointmentId,

    @NotBlank(message = "Razorpay order ID is required")
    String razorpayOrderId,

    String errorCode,
    String errorDescription
) {}
