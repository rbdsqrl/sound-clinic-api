package com.simplehearing.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public record AppointmentRequest(
    @NotNull(message = "Service ID is required")
    Long serviceId,

    @NotNull(message = "Appointment date is required")
    LocalDate appointmentDate,

    @NotBlank(message = "Time slot is required")
    String timeSlot,

    @NotBlank(message = "Patient name is required")
    String patientName,

    @NotBlank(message = "Patient phone is required")
    @Pattern(regexp = "^[+]?[0-9\\s\\-]{7,20}$", message = "Invalid phone number")
    String patientPhone,

    String notes
) {}
