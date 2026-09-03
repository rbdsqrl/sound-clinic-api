package com.simplehearing.reassignment.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/** One patient touched by a {@link TherapistReassignment} batch. */
@Entity
@Table(name = "therapist_reassignment_cases")
public class TherapistReassignmentCase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reassignment_id", nullable = false)
    private TherapistReassignment reassignment;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    /** NULL when this case had no active enrollment under the source therapist — just a caseload link. */
    @Column(name = "enrollment_id")
    private UUID enrollmentId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public TherapistReassignmentCase() {}

    public UUID getId() { return id; }

    public TherapistReassignment getReassignment() { return reassignment; }
    public void setReassignment(TherapistReassignment reassignment) { this.reassignment = reassignment; }

    public UUID getPatientId() { return patientId; }
    public void setPatientId(UUID patientId) { this.patientId = patientId; }

    public UUID getEnrollmentId() { return enrollmentId; }
    public void setEnrollmentId(UUID enrollmentId) { this.enrollmentId = enrollmentId; }

    public Instant getCreatedAt() { return createdAt; }
}
