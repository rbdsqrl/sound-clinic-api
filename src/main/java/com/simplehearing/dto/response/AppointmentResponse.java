package com.simplehearing.dto.response;

import com.simplehearing.entity.Appointment;
import com.simplehearing.enums.AppointmentStatus;
import com.simplehearing.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record AppointmentResponse(
    Long appointmentId,
    Long serviceId,
    String serviceName,
    BigDecimal servicePrice,
    LocalDate appointmentDate,
    String timeSlot,
    String patientName,
    String patientPhone,
    String notes,
    AppointmentStatus status,
    PaymentStatus paymentStatus,
    String razorpayOrderId,
    long amountInPaise,
    String currency,
    String razorpayKeyId,
    LocalDateTime createdAt
) {
    public static AppointmentResponse from(Appointment a, String razorpayKeyId) {
        PaymentStatus paymentStatus = a.getPayment() != null
            ? a.getPayment().getStatus()
            : PaymentStatus.PENDING;

        long paise = a.getService().getPriceInr()
            .multiply(BigDecimal.valueOf(100))
            .longValue();

        return new AppointmentResponse(
            a.getId(),
            a.getService().getId(),
            a.getService().getName(),
            a.getService().getPriceInr(),
            a.getAppointmentDate(),
            a.getTimeSlot(),
            a.getPatientName(),
            a.getPatientPhone(),
            a.getNotes(),
            a.getStatus(),
            paymentStatus,
            a.getRazorpayOrderId(),
            paise,
            "INR",
            razorpayKeyId,
            a.getCreatedAt()
        );
    }
}
