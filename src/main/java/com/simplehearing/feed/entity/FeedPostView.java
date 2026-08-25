package com.simplehearing.feed.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "feed_post_views")
public class FeedPostView {

    @EmbeddedId
    private FeedPostViewId id;

    @CreationTimestamp
    @Column(name = "first_viewed_at", nullable = false, updatable = false)
    private Instant firstViewedAt;

    public FeedPostView() {}

    public FeedPostView(UUID postId, UUID userId) {
        this.id = new FeedPostViewId(postId, userId);
    }

    public FeedPostViewId getId() { return id; }
    public UUID getPostId() { return id.getPostId(); }
    public UUID getUserId() { return id.getUserId(); }
    public Instant getFirstViewedAt() { return firstViewedAt; }
}
