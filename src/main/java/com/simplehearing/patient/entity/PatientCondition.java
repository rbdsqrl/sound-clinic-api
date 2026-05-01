package com.simplehearing.patient.entity;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "patient_conditions")
public class PatientCondition {

    @Embeddable
    public static class Id implements Serializable {
        private UUID patientId;
        private UUID conditionId;

        public Id() {}
        public Id(UUID patientId, UUID conditionId) {
            this.patientId = patientId;
            this.conditionId = conditionId;
        }

        public UUID getPatientId() { return patientId; }
        public UUID getConditionId() { return conditionId; }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Id id)) return false;
            return Objects.equals(patientId, id.patientId) && Objects.equals(conditionId, id.conditionId);
        }
        @Override public int hashCode() { return Objects.hash(patientId, conditionId); }
    }

    @EmbeddedId
    private Id id;

    private LocalDate diagnosedAt;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public PatientCondition() {}
    public PatientCondition(UUID patientId, UUID conditionId) {
        this.id = new Id(patientId, conditionId);
    }

    public Id getId() { return id; }
    public void setId(Id id) { this.id = id; }

    public LocalDate getDiagnosedAt() { return diagnosedAt; }
    public void setDiagnosedAt(LocalDate diagnosedAt) { this.diagnosedAt = diagnosedAt; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Instant getCreatedAt() { return createdAt; }
}
