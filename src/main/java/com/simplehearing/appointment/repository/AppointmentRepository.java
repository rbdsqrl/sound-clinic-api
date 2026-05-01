package com.simplehearing.appointment.repository;

import com.simplehearing.appointment.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    /** All appointments for a therapist within a date range (used for availability checks). */
    List<Appointment> findByTherapistIdAndAppointmentDateBetween(UUID therapistId, LocalDate from, LocalDate to);

    /** All appointments in the org, ordered by date/time. */
    List<Appointment> findByOrgIdOrderByAppointmentDateAscStartTimeAsc(UUID orgId);

    /** Appointments for a specific therapist (upcoming). */
    List<Appointment> findByTherapistIdAndOrgIdOrderByAppointmentDateAscStartTimeAsc(UUID therapistId, UUID orgId);

    /** Appointments booked by a specific parent user. */
    List<Appointment> findByBookedByAndOrgIdOrderByAppointmentDateAscStartTimeAsc(UUID bookedBy, UUID orgId);

    /** Appointments for a specific patient. */
    List<Appointment> findByPatientIdAndOrgIdOrderByAppointmentDateAscStartTimeAsc(UUID patientId, UUID orgId);

    /** Clash check: same therapist, date, start time. */
    boolean existsByTherapistIdAndAppointmentDateAndStartTime(UUID therapistId, LocalDate date, LocalTime startTime);

    Optional<Appointment> findByIdAndOrgId(UUID id, UUID orgId);
}
