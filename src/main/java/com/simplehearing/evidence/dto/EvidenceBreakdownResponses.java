package com.simplehearing.evidence.dto;

import java.util.List;
import java.util.UUID;

/** Month-by-month and child-by-child views of goal completion and video evidence. */
public final class EvidenceBreakdownResponses {
    private EvidenceBreakdownResponses() {}

    /** One therapist's goal completion in one calendar month (in the organisation's timezone). */
    public record MonthCell(String month, int goalsCompleted, int goalsWithVideo, int goalsCannotUpload, int goalsWithoutEvidence) {}

    public record MonthlyRow(UUID therapistId, String therapistName, List<MonthCell> months) {}

    /** {@code months} is every month in the window, oldest first ("2026-09"), zero-filled in each row. */
    public record Monthly(int videosRequired, List<String> months, List<MonthlyRow> rows) {}

    public record ChildRow(
            UUID patientId,
            String patientName,
            /** Programs of the therapies this child's completed goals' plans are linked to. */
            List<String> therapies,
            int activeGoals,
            int goalsCompleted,
            int goalsWithVideo,
            int goalsCannotUpload,
            int goalsWithoutEvidence,
            int videosUploaded,
            /** Sessions completed in the window — context for how much evidence to expect. */
            int sessionsCompleted
    ) {}

    public record ByChild(int videosRequired, List<ChildRow> rows) {}
}
