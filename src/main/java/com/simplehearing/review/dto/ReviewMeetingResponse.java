package com.simplehearing.review.dto;

import com.simplehearing.review.entity.ReviewMeeting;
import com.simplehearing.review.enums.ReviewMeetingStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import com.simplehearing.common.dto.ParticipantResponse;

import java.util.List;
import java.util.UUID;

public record ReviewMeetingResponse(
        UUID id,
        UUID orgId,
        UUID enrollmentId,
        UUID patientId,
        String patientName,
        UUID therapistId,
        String therapistName,
        int meetingNumber,
        LocalDate meetingDate,
        LocalTime startTime,
        LocalTime endTime,
        ReviewMeetingStatus status,

        Integer communicationRating,
        Integer progressRatingPct,
        String parentComments,
        Instant parentFeedbackAt,

        /** Admin-only — Clinic Head (also editable by Business Owner) remarks on the period
         *  under review. Never sent to a Therapist or Parent, even one who is themselves an
         *  Admin but is the treating therapist on this particular meeting. */
        String clinicHeadRemarks,
        Instant clinicHeadRemarksAt,
        String clinicHeadRemarksByName,

        String cancelledReason,
        /** Every parent linked to the patient plus the Clinic Head(s) invited. */
        List<ParticipantResponse> participants,
        Instant createdAt
) {

    /**
     * Builds the response for a given viewer.
     *
     * A parent only ever sees their own feedback, never the Clinic Head Remarks. Only
     * Admin roles (BUSINESS_OWNER/CLINIC_HEAD) see Clinic Head Remarks — and never for a
     * meeting where they are themselves the treating therapist (see the self-review guard
     * in ReviewMeetingController, which excludes such meetings before they ever reach here).
     */
    public static ReviewMeetingResponse from(ReviewMeeting m,
                                             String patientName,
                                             String therapistName,
                                             String clinicHeadRemarksByName,
                                             List<ParticipantResponse> participants,
                                             boolean canSeeParentFeedback,
                                             boolean canSeeClinicHeadRemarks) {
        return new ReviewMeetingResponse(
                m.getId(),
                m.getOrgId(),
                m.getEnrollmentId(),
                m.getPatientId(),
                patientName,
                m.getTherapistId(),
                therapistName,
                m.getMeetingNumber(),
                m.getMeetingDate(),
                m.getStartTime(),
                m.getEndTime(),
                m.getStatus(),

                canSeeParentFeedback ? m.getCommunicationRating() : null,
                canSeeParentFeedback ? m.getProgressRatingPct() : null,
                canSeeParentFeedback ? m.getParentComments() : null,
                m.getParentFeedbackAt(),

                canSeeClinicHeadRemarks ? m.getClinicHeadRemarks() : null,
                canSeeClinicHeadRemarks ? m.getClinicHeadRemarksAt() : null,
                canSeeClinicHeadRemarks ? clinicHeadRemarksByName : null,

                m.getCancelledReason(),
                participants,
                m.getCreatedAt()
        );
    }
}
