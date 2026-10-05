package com.simplehearing.evidence.dto;

import com.simplehearing.evidence.enums.EvidenceReason;

import java.util.List;
import java.util.UUID;

/**
 * Goal completion and video-evidence compliance, one row per therapist, for a date window.
 *
 * <p>A completed goal falls into exactly one bucket: enough videos ({@code goalsWithVideo}), no
 * enough videos but a recorded "couldn't upload" reason ({@code goalsCannotUpload}), or neither
 * ({@code goalsWithoutEvidence} — includes goals completed before evidence tracking began).
 */
public record EvidenceAnalyticsResponse(int videosRequired, List<Row> rows) {

    public record Row(
            UUID therapistId,
            String therapistName,
            int goalsCompleted,
            int goalsWithVideo,
            int goalsCannotUpload,
            int goalsWithoutEvidence,
            /** goalsWithVideo / goalsCompleted as a percentage, one decimal; null with no completed goals. */
            Double compliancePct,
            /** Videos uploaded in the window (any goal, session or ad hoc). */
            int videosUploaded,
            /** "Couldn't upload" records made in the window, by reason. */
            int cannotUploadRecords,
            List<ReasonCount> reasons
    ) {}

    public record ReasonCount(EvidenceReason reason, int count) {}
}
