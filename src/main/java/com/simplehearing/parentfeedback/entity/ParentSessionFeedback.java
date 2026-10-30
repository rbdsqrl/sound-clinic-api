package com.simplehearing.parentfeedback.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/** A parent's feedback on one completed session. Admin roles see it first; an admin can share it with the therapist. */
@Entity
@Table(name = "parent_session_feedback")
public class ParentSessionFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "parent_id", nullable = false)
    private UUID parentId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Null until an admin has read it (or shared it) — drives the admin dashboard card. */
    @Column(name = "admin_reviewed_at")
    private Instant adminReviewedAt;

    @Column(name = "admin_reviewed_by")
    private UUID adminReviewedBy;

    @Column(name = "shared_with_therapist", nullable = false)
    private boolean sharedWithTherapist = false;

    @Column(name = "shared_at")
    private Instant sharedAt;

    @Column(name = "shared_by")
    private UUID sharedBy;

    /** Null until the therapist has marked a shared feedback as seen — drives the therapist dashboard card. */
    @Column(name = "therapist_seen_at")
    private Instant therapistSeenAt;

    public ParentSessionFeedback() {}

    public UUID getId() { return id; }
    public UUID getOrgId() { return orgId; }
    public void setOrgId(UUID orgId) { this.orgId = orgId; }
    public UUID getSessionId() { return sessionId; }
    public void setSessionId(UUID sessionId) { this.sessionId = sessionId; }
    public UUID getPatientId() { return patientId; }
    public void setPatientId(UUID patientId) { this.patientId = patientId; }
    public UUID getParentId() { return parentId; }
    public void setParentId(UUID parentId) { this.parentId = parentId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getAdminReviewedAt() { return adminReviewedAt; }
    public void setAdminReviewedAt(Instant adminReviewedAt) { this.adminReviewedAt = adminReviewedAt; }
    public UUID getAdminReviewedBy() { return adminReviewedBy; }
    public void setAdminReviewedBy(UUID adminReviewedBy) { this.adminReviewedBy = adminReviewedBy; }
    public boolean isSharedWithTherapist() { return sharedWithTherapist; }
    public void setSharedWithTherapist(boolean sharedWithTherapist) { this.sharedWithTherapist = sharedWithTherapist; }
    public Instant getSharedAt() { return sharedAt; }
    public void setSharedAt(Instant sharedAt) { this.sharedAt = sharedAt; }
    public UUID getSharedBy() { return sharedBy; }
    public void setSharedBy(UUID sharedBy) { this.sharedBy = sharedBy; }
    public Instant getTherapistSeenAt() { return therapistSeenAt; }
    public void setTherapistSeenAt(Instant therapistSeenAt) { this.therapistSeenAt = therapistSeenAt; }
}
