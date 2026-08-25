package com.simplehearing.feed.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "feed_post_likes")
public class FeedPostLike {

    @EmbeddedId
    private FeedPostLikeId id;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public FeedPostLike() {}

    public FeedPostLike(UUID postId, UUID userId) {
        this.id = new FeedPostLikeId(postId, userId);
    }

    public FeedPostLikeId getId() { return id; }
    public UUID getPostId() { return id.getPostId(); }
    public UUID getUserId() { return id.getUserId(); }
    public Instant getCreatedAt() { return createdAt; }
}
