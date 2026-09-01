package com.simplehearing.resource.entity;

import com.simplehearing.resource.enums.ResourceType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "resources")
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "folder_id")
    private UUID folderId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private ResourceType type;

    @Column(name = "url", nullable = false, length = 2000)
    private String url;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Resource() {}

    public UUID getId()                             { return id; }
    public UUID getOrgId()                          { return orgId; }
    public void setOrgId(UUID v)                    { this.orgId = v; }
    public UUID getFolderId()                       { return folderId; }
    public void setFolderId(UUID v)                 { this.folderId = v; }
    public String getName()                         { return name; }
    public void setName(String v)                   { this.name = v; }
    public ResourceType getType()                   { return type; }
    public void setType(ResourceType v)              { this.type = v; }
    public String getUrl()                          { return url; }
    public void setUrl(String v)                    { this.url = v; }
    public UUID getCreatedBy()                      { return createdBy; }
    public void setCreatedBy(UUID v)                { this.createdBy = v; }
    public Instant getCreatedAt()                   { return createdAt; }
}
