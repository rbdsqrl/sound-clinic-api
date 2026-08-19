package com.simplehearing.review.entity;

import com.simplehearing.review.enums.ReviewMeetingStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * A scheduled feedback/review meeting between the therapist and the patient's parents,
 * sitting alongside the therapy sessions of an enrollment.
 *
 * Both sides record feedback here: the parent rates and comments on the therapist, the
 * therapist summarises the period under review.
 */
@Entity
@Table(name = "review_meetings")
public class ReviewMeeting {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "enrollment_id", nullable = false)
    private UUID enrollmentId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "therapist_id", nullable = false)
    private UUID therapistId;

    @Column(name = "meeting_number", nullable = false)
    private int meetingNumber;

    @Column(name = "meeting_date", nullable = false)
    private LocalDate meetingDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewMeetingStatus status = ReviewMeetingStatus.SCHEDULED;

    // ── Parent's feedback about the therapist ────────────────────────────────

    @Column(name = "parent_rating")
    private Integer parentRating;

    @Column(name = "parent_comments", columnDefinition = "TEXT")
    private String parentComments;

    @Column(name = "parent_feedback_by")
    private UUID parentFeedbackBy;

    @Column(name = "parent_feedback_at")
    private Instant parentFeedbackAt;

    // ── Therapist's feedback about the period ────────────────────────────────

    @Column(name = "therapist_summary", columnDefinition = "TEXT")
    private String therapistSummary;

    @Column(name = "therapist_progress_notes", columnDefinition = "TEXT")
    private String therapistProgressNotes;

    @Column(name = "therapist_feedback_at")
    private Instant therapistFeedbackAt;

    // ── Calendar invite bookkeeping ──────────────────────────────────────────

    /** Bumped on every reschedule so calendar clients update the event instead of adding a second one. */
    @Column(name = "ics_sequence", nullable = false)
    private int icsSequence = 0;

    /** Stable across the meeting's life — the identity calendar clients match on. */
    @Column(name = "ics_uid", nullable = false)
    private String icsUid;

    @Column(name = "cancelled_reason", columnDefinition = "TEXT")
    private String cancelledReason;

    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public ReviewMeeting() {}

    public UUID getId() { return id; }

    public UUID getOrgId() { return orgId; }
    public void setOrgId(UUID orgId) { this.orgId = orgId; }

    public UUID getEnrollmentId() { return enrollmentId; }
    public void setEnrollmentId(UUID enrollmentId) { this.enrollmentId = enrollmentId; }

    public UUID getPatientId() { return patientId; }
    public void setPatientId(UUID patientId) { this.patientId = patientId; }

    public UUID getTherapistId() { return therapistId; }
    public void setTherapistId(UUID therapistId) { this.therapistId = therapistId; }

    public int getMeetingNumber() { return meetingNumber; }
    public void setMeetingNumber(int meetingNumber) { this.meetingNumber = meetingNumber; }

    public LocalDate getMeetingDate() { return meetingDate; }
    public void setMeetingDate(LocalDate meetingDate) { this.meetingDate = meetingDate; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public ReviewMeetingStatus getStatus() { return status; }
    public void setStatus(ReviewMeetingStatus status) { this.status = status; }

    public Integer getParentRating() { return parentRating; }
    public void setParentRating(Integer parentRating) { this.parentRating = parentRating; }

    public String getParentComments() { return parentComments; }
    public void setParentComments(String parentComments) { this.parentComments = parentComments; }

    public UUID getParentFeedbackBy() { return parentFeedbackBy; }
    public void setParentFeedbackBy(UUID parentFeedbackBy) { this.parentFeedbackBy = parentFeedbackBy; }

    public Instant getParentFeedbackAt() { return parentFeedbackAt; }
    public void setParentFeedbackAt(Instant parentFeedbackAt) { this.parentFeedbackAt = parentFeedbackAt; }

    public String getTherapistSummary() { return therapistSummary; }
    public void setTherapistSummary(String therapistSummary) { this.therapistSummary = therapistSummary; }

    public String getTherapistProgressNotes() { return therapistProgressNotes; }
    public void setTherapistProgressNotes(String therapistProgressNotes) { this.therapistProgressNotes = therapistProgressNotes; }

    public Instant getTherapistFeedbackAt() { return therapistFeedbackAt; }
    public void setTherapistFeedbackAt(Instant therapistFeedbackAt) { this.therapistFeedbackAt = therapistFeedbackAt; }

    public int getIcsSequence() { return icsSequence; }
    public void setIcsSequence(int icsSequence) { this.icsSequence = icsSequence; }

    public String getIcsUid() { return icsUid; }
    public void setIcsUid(String icsUid) { this.icsUid = icsUid; }

    public String getCancelledReason() { return cancelledReason; }
    public void setCancelledReason(String cancelledReason) { this.cancelledReason = cancelledReason; }

    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    /** True once the parent has submitted their side. */
    public boolean hasParentFeedback() { return parentFeedbackAt != null; }

    /** True once the therapist has submitted their side. */
    public boolean hasTherapistFeedback() { return therapistFeedbackAt != null; }
}
