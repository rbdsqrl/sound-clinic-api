package com.simplehearing.patient.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "therapist_patients")
public class TherapistPatient {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "therapist_id", nullable = false)
    private UUID therapistId;

    @Column(name = "assigned_by", nullable = false)
    private UUID assignedBy;

    @Column(nullable = false)
    private Instant assignedAt = Instant.now();

    @Column(nullable = false)
    private boolean isActive = true;

    public TherapistPatient() {}

    public UUID getId() { return id; }

    public UUID getPatientId() { return patientId; }
    public void setPatientId(UUID patientId) { this.patientId = patientId; }

    public UUID getTherapistId() { return therapistId; }
    public void setTherapistId(UUID therapistId) { this.therapistId = therapistId; }

    public UUID getAssignedBy() { return assignedBy; }
    public void setAssignedBy(UUID assignedBy) { this.assignedBy = assignedBy; }

    public Instant getAssignedAt() { return assignedAt; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}
