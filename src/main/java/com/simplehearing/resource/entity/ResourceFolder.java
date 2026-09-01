package com.simplehearing.resource.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "resource_folders")
public class ResourceFolder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "parent_folder_id")
    private UUID parentFolderId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public ResourceFolder() {}

    public UUID getId()                             { return id; }
    public UUID getOrgId()                          { return orgId; }
    public void setOrgId(UUID v)                    { this.orgId = v; }
    public UUID getParentFolderId()                 { return parentFolderId; }
    public void setParentFolderId(UUID v)           { this.parentFolderId = v; }
    public String getName()                         { return name; }
    public void setName(String v)                   { this.name = v; }
    public UUID getCreatedBy()                      { return createdBy; }
    public void setCreatedBy(UUID v)                { this.createdBy = v; }
    public Instant getCreatedAt()                   { return createdAt; }
}
