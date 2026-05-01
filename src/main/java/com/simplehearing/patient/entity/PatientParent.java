package com.simplehearing.patient.entity;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "patient_parents")
public class PatientParent {

    @Embeddable
    public static class Id implements Serializable {
        private UUID patientId;
        private UUID parentId;

        public Id() {}
        public Id(UUID patientId, UUID parentId) {
            this.patientId = patientId;
            this.parentId = parentId;
        }

        public UUID getPatientId() { return patientId; }
        public UUID getParentId() { return parentId; }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Id id)) return false;
            return Objects.equals(patientId, id.patientId) && Objects.equals(parentId, id.parentId);
        }
        @Override public int hashCode() { return Objects.hash(patientId, parentId); }
    }

    @EmbeddedId
    private Id id;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public PatientParent() {}
    public PatientParent(UUID patientId, UUID parentId) {
        this.id = new Id(patientId, parentId);
    }

    public Id getId() { return id; }
    public void setId(Id id) { this.id = id; }
    public Instant getCreatedAt() { return createdAt; }
}
