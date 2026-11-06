package com.simplehearing.attendance.repository;

import com.simplehearing.attendance.entity.AttendanceEditEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AttendanceEditEntryRepository extends JpaRepository<AttendanceEditEntry, UUID> {

    List<AttendanceEditEntry> findByAttendanceIdOrderByEditedAtDesc(UUID attendanceId);
}
