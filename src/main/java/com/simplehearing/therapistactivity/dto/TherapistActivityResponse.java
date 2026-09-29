package com.simplehearing.therapistactivity.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * One therapist's documentation activity over a date range (a single day is just from == to) —
 * a management/compliance check ("did they actually keep notes and share media"), not a trend
 * chart. Three independent metrics:
 *
 * <ul>
 *   <li><b>Therapy session notes</b> — a session in range with {@code notes}/{@code progressReport}/
 *       {@code feedback} filled in.</li>
 *   <li><b>Free-text notes</b> — a Media & Notes entry in range carrying a {@code note}.</li>
 *   <li><b>Media</b> — a Media & Notes entry in range carrying a {@code fileUrl}.</li>
 * </ul>
 *
 * The same Media & Notes row can appear in both the free-text and media breakdowns if it carries
 * both — the two are independent counts, not a partition.
 */
public record TherapistActivityResponse(
        UUID therapistId,
        String therapistName,
        LocalDate from,
        LocalDate to,

        int therapySessionNotesCount,
        List<ChildSessionNotes> therapySessionNotesByChild,

        int freeTextNotesCount,
        List<ChildFreeTextNotes> freeTextNotesByChild,

        int mediaCount,
        List<ChildMedia> mediaByChild
) {

    public record ChildSessionNotes(UUID patientId, String patientName, List<SessionNoteEntry> entries) {}

    public record SessionNoteEntry(
            UUID sessionId,
            LocalDate sessionDate,
            LocalTime startTime,
            String notes,
            String progressReport,
            String feedback,
            Integer performanceScore
    ) {}

    public record ChildFreeTextNotes(UUID patientId, String patientName, List<FreeTextNoteEntry> entries) {}

    public record FreeTextNoteEntry(UUID id, Instant createdAt, String note) {}

    public record ChildMedia(UUID patientId, String patientName, List<MediaEntry> entries) {}

    public record MediaEntry(
            UUID id,
            Instant createdAt,
            String fileUrl,
            String fileName,
            String contentType,
            /** The same entry's note, if it also carries one (shown for context, not counted here). */
            String note
    ) {}
}
