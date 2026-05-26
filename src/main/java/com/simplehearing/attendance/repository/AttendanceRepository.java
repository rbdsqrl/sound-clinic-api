package com.simplehearing.attendance.repository;

import com.simplehearing.attendance.entity.Attendance;
import com.simplehearing.attendance.enums.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttendanceRepository extends JpaRepository<Attendance, UUID> {

    Optional<Attendance> findByUserIdAndAttendanceDate(UUID userId, LocalDate date);

    Optional<Attendance> findByUserIdAndAttendanceDateAndStatus(UUID userId, LocalDate date, AttendanceStatus status);

    List<Attendance> findByOrgIdAndAttendanceDateOrderByCheckInTimeDesc(UUID orgId, LocalDate date);

    List<Attendance> findByUserIdOrderByAttendanceDateDesc(UUID userId);

    List<Attendance> findByOrgIdAndAttendanceDateBetweenOrderByAttendanceDateDescCheckInTimeDesc(
            UUID orgId, LocalDate from, LocalDate to);
}
