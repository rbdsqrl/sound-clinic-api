package com.simplehearing.iep.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/** A domain an organisation added itself for IEP goals — see migration 122. */
@Entity
@Table(name = "iep_custom_domains")
public class IEPCustomDomain {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public IEPCustomDomain() {}

    public UUID getId()                   { return id; }
    public UUID getOrgId()                { return orgId; }
    public void setOrgId(UUID v)          { this.orgId = v; }
    public String getName()               { return name; }
    public void setName(String v)         { this.name = v; }
    public UUID getCreatedBy()            { return createdBy; }
    public void setCreatedBy(UUID v)      { this.createdBy = v; }
    public Instant getCreatedAt()         { return createdAt; }
}
