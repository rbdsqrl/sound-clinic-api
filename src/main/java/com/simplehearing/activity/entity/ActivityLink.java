package com.simplehearing.activity.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "activity_links")
public class ActivityLink {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "activity_id", nullable = false)
    private UUID activityId;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @Column(nullable = false, length = 1000)
    private String url;

    public ActivityLink() {}

    public UUID getId() { return id; }
    public UUID getActivityId() { return activityId; }
    public void setActivityId(UUID activityId) { this.activityId = activityId; }
    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
}
