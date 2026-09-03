package com.simplehearing.reassignment.entity;

import com.simplehearing.reassignment.enums.ReassignmentStatus;
import com.simplehearing.reassignment.enums.ReassignmentType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** One bulk hand-off of a therapist's cases to another therapist — permanent, or bounded to a window. */
@Entity
@Table(name = "therapist_reassignments")
public class TherapistReassignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "from_therapist_id", nullable = false)
    private UUID fromTherapistId;

    @Column(name = "to_therapist_id", nullable = false)
    private UUID toTherapistId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReassignmentType type;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /** NULL for PERMANENT; required for TEMPORARY — when the covered cases hand back automatically. */
    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReassignmentStatus status = ReassignmentStatus.ACTIVE;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "reverted_at")
    private Instant revertedAt;

    /** Set only on an early manual cancel; NULL when the nightly job reverts it automatically. */
    @Column(name = "reverted_by")
    private UUID revertedBy;

    public TherapistReassignment() {}

    public UUID getId() { return id; }

    public UUID getOrgId() { return orgId; }
    public void setOrgId(UUID orgId) { this.orgId = orgId; }

    public UUID getFromTherapistId() { return fromTherapistId; }
    public void setFromTherapistId(UUID fromTherapistId) { this.fromTherapistId = fromTherapistId; }

    public UUID getToTherapistId() { return toTherapistId; }
    public void setToTherapistId(UUID toTherapistId) { this.toTherapistId = toTherapistId; }

    public ReassignmentType getType() { return type; }
    public void setType(ReassignmentType type) { this.type = type; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public ReassignmentStatus getStatus() { return status; }
    public void setStatus(ReassignmentStatus status) { this.status = status; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }

    public Instant getCreatedAt() { return createdAt; }

    public Instant getRevertedAt() { return revertedAt; }
    public void setRevertedAt(Instant revertedAt) { this.revertedAt = revertedAt; }

    public UUID getRevertedBy() { return revertedBy; }
    public void setRevertedBy(UUID revertedBy) { this.revertedBy = revertedBy; }
}
