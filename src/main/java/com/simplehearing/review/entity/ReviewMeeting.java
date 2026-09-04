package com.simplehearing.review.entity;

import com.simplehearing.review.enums.ReviewMeetingStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * A scheduled feedback/review meeting between the therapist and the patient's parents,
 * sitting alongside the therapy sessions of an enrollment.
 *
 * The parent rates and comments on the therapist here; separately, a Clinic Head (or
 * Business Owner) can leave confidential remarks on the period under review — visible only
 * to Admin roles, never the therapist or the parent.
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

    /** @deprecated superseded by {@link #communicationRating}; kept for historical rows, no longer written. */
    @Deprecated
    @Column(name = "parent_rating")
    private Integer parentRating;

    /** 1-5 stars — how the parent feels about the therapist relationship/interaction. */
    @Column(name = "communication_rating")
    private Integer communicationRating;

    /** 0-100 — how much progress the parent perceives. */
    @Column(name = "progress_rating_pct")
    private Integer progressRatingPct;

    @Column(name = "parent_comments", columnDefinition = "TEXT")
    private String parentComments;

    @Column(name = "parent_feedback_by")
    private UUID parentFeedbackBy;

    @Column(name = "parent_feedback_at")
    private Instant parentFeedbackAt;

    // ── Clinic Head's confidential remarks on the therapist/period ───────────
    // Admin-only (Clinic Head, also editable by Business Owner) — never visible to the
    // Therapist or Parent, even a Clinic Head/Business Owner who is themselves the treating
    // therapist on this particular meeting. See ReviewMeetingController's self-review guard.

    @Column(name = "clinic_head_remarks", columnDefinition = "TEXT")
    private String clinicHeadRemarks;

    @Column(name = "clinic_head_remarks_at")
    private Instant clinicHeadRemarksAt;

    @Column(name = "clinic_head_remarks_by")
    private UUID clinicHeadRemarksBy;

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

    /** Set while a bulk therapist reassignment owns this row's therapistId; cleared on revert. */
    @Column(name = "reassignment_id")
    private UUID reassignmentId;

    /**
     * Who is invited: the patient's linked parents plus the Clinic Head(s) chosen at
     * scheduling time. The assigned therapist is deliberately not a participant — {@code
     * therapistId} above is kept purely for clinical/analytics attribution.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "review_meeting_participants", joinColumns = @JoinColumn(name = "review_meeting_id"))
    @Column(name = "user_id", nullable = false)
    private Set<UUID> participantIds = new LinkedHashSet<>();

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

    public Integer getCommunicationRating() { return communicationRating; }
    public void setCommunicationRating(Integer communicationRating) { this.communicationRating = communicationRating; }

    public Integer getProgressRatingPct() { return progressRatingPct; }
    public void setProgressRatingPct(Integer progressRatingPct) { this.progressRatingPct = progressRatingPct; }

    public String getParentComments() { return parentComments; }
    public void setParentComments(String parentComments) { this.parentComments = parentComments; }

    public UUID getParentFeedbackBy() { return parentFeedbackBy; }
    public void setParentFeedbackBy(UUID parentFeedbackBy) { this.parentFeedbackBy = parentFeedbackBy; }

    public Instant getParentFeedbackAt() { return parentFeedbackAt; }
    public void setParentFeedbackAt(Instant parentFeedbackAt) { this.parentFeedbackAt = parentFeedbackAt; }

    public String getClinicHeadRemarks() { return clinicHeadRemarks; }
    public void setClinicHeadRemarks(String clinicHeadRemarks) { this.clinicHeadRemarks = clinicHeadRemarks; }

    public Instant getClinicHeadRemarksAt() { return clinicHeadRemarksAt; }
    public void setClinicHeadRemarksAt(Instant clinicHeadRemarksAt) { this.clinicHeadRemarksAt = clinicHeadRemarksAt; }

    public UUID getClinicHeadRemarksBy() { return clinicHeadRemarksBy; }
    public void setClinicHeadRemarksBy(UUID clinicHeadRemarksBy) { this.clinicHeadRemarksBy = clinicHeadRemarksBy; }

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

    public UUID getReassignmentId() { return reassignmentId; }
    public void setReassignmentId(UUID reassignmentId) { this.reassignmentId = reassignmentId; }

    public Set<UUID> getParticipantIds() { return participantIds; }
    public void setParticipantIds(Set<UUID> participantIds) { this.participantIds = participantIds; }

    /** True once the parent has submitted their side. */
    public boolean hasParentFeedback() { return parentFeedbackAt != null; }

    /** True once a Clinic Head/Business Owner has written remarks. */
    public boolean hasClinicHeadRemarks() { return clinicHeadRemarksAt != null; }
}
