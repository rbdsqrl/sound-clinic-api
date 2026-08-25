package com.simplehearing.feed.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class FeedPostLikeId implements Serializable {

    @Column(name = "post_id")
    private UUID postId;

    @Column(name = "user_id")
    private UUID userId;

    public FeedPostLikeId() {}

    public FeedPostLikeId(UUID postId, UUID userId) {
        this.postId = postId;
        this.userId = userId;
    }

    public UUID getPostId() { return postId; }
    public UUID getUserId() { return userId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FeedPostLikeId)) return false;
        FeedPostLikeId that = (FeedPostLikeId) o;
        return Objects.equals(postId, that.postId) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() { return Objects.hash(postId, userId); }
}
