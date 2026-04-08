package com.simplehearing.repository;

import com.simplehearing.entity.Appointment;
import com.simplehearing.enums.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    Page<Appointment> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Appointment> findByStatusOrderByCreatedAtDesc(AppointmentStatus status, Pageable pageable);

    Page<Appointment> findByAppointmentDateOrderByTimeSlotAsc(LocalDate date, Pageable pageable);

    @Query("SELECT a.timeSlot FROM Appointment a WHERE a.appointmentDate = :date AND a.status != 'CANCELLED'")
    List<String> findBookedSlotsByDate(@Param("date") LocalDate date);

    Optional<Appointment> findByRazorpayOrderId(String razorpayOrderId);

    boolean existsByAppointmentDateAndTimeSlotAndStatusNot(
        LocalDate date, String timeSlot, AppointmentStatus status);
}
