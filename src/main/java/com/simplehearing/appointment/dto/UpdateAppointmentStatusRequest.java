package com.simplehearing.appointment.dto;

import com.simplehearing.appointment.enums.AppointmentStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateAppointmentStatusRequest(@NotNull AppointmentStatus status) {}
