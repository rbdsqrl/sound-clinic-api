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

        String therapistSummary,
        String therapistProgressNotes,
        Instant therapistFeedbackAt,

        String cancelledReason,
        /** Therapist plus every parent linked to the patient. */
        List<ParticipantResponse> participants,
        Instant createdAt
) {

    /**
     * Builds the response for a given viewer.
     *
     * A parent only ever sees their own feedback, never the therapist's; a therapist only
     * ever sees their own, never the parent's. Only staff (BUSINESS_OWNER/CLINIC_HEAD) see
     * both sides.
     */
    public static ReviewMeetingResponse from(ReviewMeeting m,
                                             String patientName,
                                             String therapistName,
                                             List<ParticipantResponse> participants,
                                             boolean canSeeParentFeedback,
                                             boolean canSeeTherapistFeedback) {
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

                canSeeTherapistFeedback ? m.getTherapistSummary() : null,
                canSeeTherapistFeedback ? m.getTherapistProgressNotes() : null,
                m.getTherapistFeedbackAt(),

                m.getCancelledReason(),
                participants,
                m.getCreatedAt()
        );
    }
}
