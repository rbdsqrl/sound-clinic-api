package com.simplehearing.appointment.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record BookAppointmentRequest(
        @NotNull UUID patientId,
        @NotNull UUID therapistId,
        @NotNull LocalDate appointmentDate,
        @NotNull LocalTime startTime,
        String notes
) {}
