package com.simplehearing.attendance.dto;

import com.simplehearing.attendance.entity.Attendance;
import com.simplehearing.attendance.enums.AttendanceStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AttendanceResponse(
        UUID id,
        UUID userId,
        String userFirstName,
        String userLastName,
        UUID clinicId,
        String clinicName,
        LocalDate attendanceDate,
        Instant checkInTime,
        Instant checkOutTime,
        boolean geoVerified,
        boolean faceVerified,
        AttendanceStatus status,
        Instant createdAt
) {
    public static AttendanceResponse from(
            Attendance a,
            String userFirstName,
            String userLastName,
            String clinicName) {
        return new AttendanceResponse(
                a.getId(),
                a.getUserId(),
                userFirstName,
                userLastName,
                a.getClinicId(),
                clinicName,
                a.getAttendanceDate(),
                a.getCheckInTime(),
                a.getCheckOutTime(),
                a.isGeoVerified(),
                a.isFaceVerified(),
                a.getStatus(),
                a.getCreatedAt()
        );
    }
}
