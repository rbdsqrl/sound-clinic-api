package com.simplehearing.dto.response;

import com.simplehearing.entity.Payment;
import com.simplehearing.enums.AppointmentStatus;
import com.simplehearing.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
    Long paymentId,
    Long appointmentId,
    String razorpayOrderId,
    String razorpayPaymentId,
    BigDecimal amount,
    PaymentStatus status,
    AppointmentStatus appointmentStatus,
    LocalDateTime paidAt
) {
    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(
            p.getId(),
            p.getAppointment().getId(),
            p.getRazorpayOrderId(),
            p.getRazorpayPaymentId(),
            p.getAmount(),
            p.getStatus(),
            p.getAppointment().getStatus(),
            p.getPaidAt()
        );
    }
}
