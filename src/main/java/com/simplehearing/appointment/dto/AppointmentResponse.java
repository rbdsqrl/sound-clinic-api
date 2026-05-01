package com.simplehearing.appointment.dto;

import com.simplehearing.appointment.entity.Appointment;
import com.simplehearing.appointment.enums.AppointmentStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record AppointmentResponse(
        UUID id,
        UUID patientId,
        String patientFirstName,
        String patientLastName,
        UUID therapistId,
        String therapistFirstName,
        String therapistLastName,
        UUID clinicId,
        String clinicName,
        LocalDate appointmentDate,
        LocalTime startTime,
        LocalTime endTime,
        AppointmentStatus status,
        String notes,
        UUID bookedBy,
        Instant createdAt
) {
    public static AppointmentResponse from(
            Appointment a,
            String patientFirst, String patientLast,
            String therapistFirst, String therapistLast,
            String clinicName) {
        return new AppointmentResponse(
                a.getId(), a.getPatientId(), patientFirst, patientLast,
                a.getTherapistId(), therapistFirst, therapistLast,
                a.getClinicId(), clinicName,
                a.getAppointmentDate(), a.getStartTime(), a.getEndTime(),
                a.getStatus(), a.getNotes(), a.getBookedBy(), a.getCreatedAt()
        );
    }
}
