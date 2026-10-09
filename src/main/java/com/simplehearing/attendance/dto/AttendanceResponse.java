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
        /** The clinic's name — or the organisation's, when {@code atOrganisation}. */
        String clinicName,
        boolean atOrganisation,
        LocalDate attendanceDate,
        Instant checkInTime,
        Instant checkOutTime,
        boolean geoVerified,
        boolean faceVerified,
        boolean faceOverride,
        Boolean overrideApproved,
        String overrideReviewedByName,
        Instant overrideReviewedAt,
        AttendanceStatus status,
        Instant createdAt
) {
    public static AttendanceResponse from(
            Attendance a,
            String userFirstName,
            String userLastName,
            String clinicName,
            String overrideReviewedByName) {
        return new AttendanceResponse(
                a.getId(),
                a.getUserId(),
                userFirstName,
                userLastName,
                a.getClinicId(),
                clinicName,
                a.isAtOrganisation(),
                a.getAttendanceDate(),
                a.getCheckInTime(),
                a.getCheckOutTime(),
                a.isGeoVerified(),
                a.isFaceVerified(),
                a.isFaceOverride(),
                a.getOverrideApproved(),
                overrideReviewedByName,
                a.getOverrideReviewedAt(),
                a.getStatus(),
                a.getCreatedAt()
        );
    }
}
