package com.simplehearing.discharge.dto;

import com.simplehearing.discharge.entity.DischargeRecord;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DischargeRecordResponse(
        UUID id,
        UUID patientId,
        LocalDate dischargeDate,
        UUID dischargedBy,
        String dischargedByName,
        LocalDate episodeStartDate,
        BigDecimal avgCommunicationRating,
        BigDecimal avgProgressRatingPct,
        BigDecimal goalMasteryPct,
        Boolean goalMasteryMet,
        boolean therapistSignoffMet,
        Boolean parentSatisfactionMet,
        boolean overallSuccessful,
        String notes,
        boolean pdfAvailable,
        List<EnrollmentSummary> enrollments,
        Instant createdAt
) {
    public record EnrollmentSummary(
            UUID enrollmentId, String programName, String therapistName, LocalDate startDate, LocalDate endDate) {}

    public static DischargeRecordResponse from(
            DischargeRecord d, String dischargedByName, List<EnrollmentSummary> enrollments) {
        return new DischargeRecordResponse(
                d.getId(),
                d.getPatientId(),
                d.getDischargeDate(),
                d.getDischargedBy(),
                dischargedByName,
                d.getEpisodeStartDate(),
                d.getAvgCommunicationRating(),
                d.getAvgProgressRatingPct(),
                d.getGoalMasteryPct(),
                d.getGoalMasteryMet(),
                d.isTherapistSignoffMet(),
                d.getParentSatisfactionMet(),
                d.isOverallSuccessful(),
                d.getNotes(),
                d.getPdfUrl() != null,
                enrollments,
                d.getCreatedAt()
        );
    }
}
