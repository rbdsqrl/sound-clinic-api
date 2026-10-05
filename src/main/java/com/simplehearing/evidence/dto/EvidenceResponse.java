package com.simplehearing.evidence.dto;

import com.simplehearing.evidence.entity.GoalEvidence;
import com.simplehearing.evidence.enums.EvidenceKind;
import com.simplehearing.evidence.enums.EvidenceReason;

import java.time.Instant;
import java.util.UUID;

public record EvidenceResponse(
        UUID id,
        UUID patientId,
        UUID planId,
        UUID goalId,
        UUID sessionId,
        EvidenceKind kind,
        /** "IEP Plan - Goal" for goal evidence; a generic label for ad hoc evidence. */
        String title,
        String planTitle,
        String goalTitle,
        String fileName,
        /** Short-lived presigned download/stream URL — never the stored URL. Null for CANNOT_UPLOAD. */
        String fileUrl,
        String contentType,
        Long fileSizeBytes,
        Integer durationSeconds,
        EvidenceReason reasonCode,
        String reasonText,
        String note,
        UUID therapistId,
        String therapistName,
        Instant createdAt
) {
    public static EvidenceResponse from(GoalEvidence e, String planTitle, String goalTitle,
                                        String therapistName, String presignedUrl) {
        String title = goalTitle != null
                ? (planTitle != null ? planTitle + " - " + goalTitle : goalTitle)
                : (e.getSessionId() != null ? "Session evidence" : "General evidence");
        return new EvidenceResponse(
                e.getId(), e.getPatientId(), e.getPlanId(), e.getGoalId(), e.getSessionId(), e.getKind(),
                title, planTitle, goalTitle, e.getFileName(), presignedUrl, e.getContentType(),
                e.getFileSizeBytes(), e.getDurationSeconds(), e.getReasonCode(), e.getReasonText(), e.getNote(),
                e.getTherapistId(), therapistName, e.getCreatedAt());
    }
}
