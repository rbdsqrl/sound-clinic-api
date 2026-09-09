package com.simplehearing.resource.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/** A Resources-library item assigned to one patient — what actually shows up in the Parent app,
 *  which never sees the org-wide library, only what's been assigned to their own child. */
@Entity
@Table(name = "resource_assignments")
public class ResourceAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "assigned_by", nullable = false)
    private UUID assignedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public ResourceAssignment() {}

    public UUID getId()                           { return id; }
    public UUID getOrgId()                        { return orgId; }
    public void setOrgId(UUID v)                  { this.orgId = v; }
    public UUID getResourceId()                   { return resourceId; }
    public void setResourceId(UUID v)             { this.resourceId = v; }
    public UUID getPatientId()                    { return patientId; }
    public void setPatientId(UUID v)              { this.patientId = v; }
    public UUID getAssignedBy()                   { return assignedBy; }
    public void setAssignedBy(UUID v)             { this.assignedBy = v; }
    public Instant getCreatedAt()                 { return createdAt; }
}
