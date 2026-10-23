package com.simplehearing.subscription.entity;

import com.simplehearing.subscription.enums.PaymentMode;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** One payment taken against a plan. A negative amount is a correction to the running total. */
@Entity
@Table(name = "subscription_payments")
public class SubscriptionPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "subscription_id", nullable = false)
    private UUID subscriptionId;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "paid_on", nullable = false)
    private LocalDate paidOn;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", length = 30)
    private PaymentMode mode;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "recorded_by")
    private UUID recordedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public SubscriptionPayment() {}

    public UUID getId()                         { return id; }
    public UUID getOrgId()                      { return orgId; }
    public void setOrgId(UUID v)                { this.orgId = v; }
    public UUID getSubscriptionId()             { return subscriptionId; }
    public void setSubscriptionId(UUID v)       { this.subscriptionId = v; }
    public BigDecimal getAmount()               { return amount; }
    public void setAmount(BigDecimal v)         { this.amount = v; }
    public LocalDate getPaidOn()                { return paidOn; }
    public void setPaidOn(LocalDate v)          { this.paidOn = v; }
    public PaymentMode getMode()                { return mode; }
    public void setMode(PaymentMode v)          { this.mode = v; }
    public String getNote()                     { return note; }
    public void setNote(String v)               { this.note = v; }
    public UUID getRecordedBy()                 { return recordedBy; }
    public void setRecordedBy(UUID v)           { this.recordedBy = v; }
    public Instant getCreatedAt()               { return createdAt; }
}
