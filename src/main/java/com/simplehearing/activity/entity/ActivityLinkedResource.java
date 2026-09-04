package com.simplehearing.activity.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/** Links an Activity to an item in the org-wide Resources library (com.simplehearing.resource) —
 *  distinct from {@link ActivityResource}, which is a file uploaded directly to one activity. */
@Entity
@Table(name = "activity_linked_resources")
public class ActivityLinkedResource {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "activity_id", nullable = false)
    private UUID activityId;

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public ActivityLinkedResource() {}

    public UUID getId()                          { return id; }
    public UUID getActivityId()                  { return activityId; }
    public void setActivityId(UUID activityId)   { this.activityId = activityId; }
    public UUID getResourceId()                  { return resourceId; }
    public void setResourceId(UUID resourceId)   { this.resourceId = resourceId; }
    public int getOrderIndex()                   { return orderIndex; }
    public void setOrderIndex(int orderIndex)    { this.orderIndex = orderIndex; }
    public Instant getCreatedAt()                { return createdAt; }
}
