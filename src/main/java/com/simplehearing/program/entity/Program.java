package com.simplehearing.program.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "programs")
public class Program {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "per_session_cost", nullable = false, precision = 10, scale = 2)
    private BigDecimal perSessionCost;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "tax_id")
    private UUID taxId;

    @Column(name = "price_includes_tax", nullable = false)
    private boolean priceIncludesTax = true;

    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Program() {}

    public UUID getId() { return id; }
    public UUID getOrgId() { return orgId; }
    public void setOrgId(UUID orgId) { this.orgId = orgId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPerSessionCost() { return perSessionCost; }
    public void setPerSessionCost(BigDecimal perSessionCost) { this.perSessionCost = perSessionCost; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public UUID getTaxId() { return taxId; }
    public void setTaxId(UUID taxId) { this.taxId = taxId; }
    public boolean isPriceIncludesTax() { return priceIncludesTax; }
    public void setPriceIncludesTax(boolean priceIncludesTax) { this.priceIncludesTax = priceIncludesTax; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
