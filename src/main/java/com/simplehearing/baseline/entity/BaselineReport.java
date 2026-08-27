package com.simplehearing.baseline.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "baseline_reports")
public class BaselineReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "patient_id", nullable = false, unique = true)
    private UUID patientId;

    @Column(name = "age_at_admission", length = 50)
    private String ageAtAdmission;

    @Column(name = "age_on_date", length = 50)
    private String ageOnDate;

    @Column(name = "cdct", length = 100)
    private String cdct;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public BaselineReport() {}

    public UUID getId() { return id; }

    public UUID getOrgId() { return orgId; }
    public void setOrgId(UUID orgId) { this.orgId = orgId; }

    public UUID getPatientId() { return patientId; }
    public void setPatientId(UUID patientId) { this.patientId = patientId; }

    public String getAgeAtAdmission() { return ageAtAdmission; }
    public void setAgeAtAdmission(String ageAtAdmission) { this.ageAtAdmission = ageAtAdmission; }

    public String getAgeOnDate() { return ageOnDate; }
    public void setAgeOnDate(String ageOnDate) { this.ageOnDate = ageOnDate; }

    public String getCdct() { return cdct; }
    public void setCdct(String cdct) { this.cdct = cdct; }

    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
