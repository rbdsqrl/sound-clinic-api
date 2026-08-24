package com.simplehearing.concern.entity;

import com.simplehearing.concern.enums.ConcernStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "enrollment_concerns")
public class EnrollmentConcern {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "enrollment_id", nullable = false)
    private UUID enrollmentId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    /** Denormalised from the enrollment at raise time — the therapist assignment may change later. */
    @Column(name = "therapist_id", nullable = false)
    private UUID therapistId;

    @Column(name = "raised_by", nullable = false)
    private UUID raisedBy;

    @Column(name = "raised_at", nullable = false)
    private Instant raisedAt;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConcernStatus status = ConcernStatus.OPEN;

    @Column(name = "acknowledged_by")
    private UUID acknowledgedBy;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "resolved_by")
    private UUID resolvedBy;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public EnrollmentConcern() {}

    public UUID getId() { return id; }
    public UUID getOrgId() { return orgId; }
    public void setOrgId(UUID orgId) { this.orgId = orgId; }
    public UUID getEnrollmentId() { return enrollmentId; }
    public void setEnrollmentId(UUID enrollmentId) { this.enrollmentId = enrollmentId; }
    public UUID getPatientId() { return patientId; }
    public void setPatientId(UUID patientId) { this.patientId = patientId; }
    public UUID getTherapistId() { return therapistId; }
    public void setTherapistId(UUID therapistId) { this.therapistId = therapistId; }
    public UUID getRaisedBy() { return raisedBy; }
    public void setRaisedBy(UUID raisedBy) { this.raisedBy = raisedBy; }
    public Instant getRaisedAt() { return raisedAt; }
    public void setRaisedAt(Instant raisedAt) { this.raisedAt = raisedAt; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public ConcernStatus getStatus() { return status; }
    public void setStatus(ConcernStatus status) { this.status = status; }
    public UUID getAcknowledgedBy() { return acknowledgedBy; }
    public void setAcknowledgedBy(UUID acknowledgedBy) { this.acknowledgedBy = acknowledgedBy; }
    public Instant getAcknowledgedAt() { return acknowledgedAt; }
    public void setAcknowledgedAt(Instant acknowledgedAt) { this.acknowledgedAt = acknowledgedAt; }
    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }
    public UUID getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(UUID resolvedBy) { this.resolvedBy = resolvedBy; }
    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
