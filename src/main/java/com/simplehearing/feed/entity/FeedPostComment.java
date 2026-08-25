package com.simplehearing.feed.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "feed_post_comments")
public class FeedPostComment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "post_id", nullable = false)
    private UUID postId;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public UUID getId()                      { return id; }
    public UUID getOrgId()                   { return orgId; }
    public void setOrgId(UUID orgId)         { this.orgId = orgId; }
    public UUID getPostId()                  { return postId; }
    public void setPostId(UUID postId)       { this.postId = postId; }
    public UUID getAuthorId()                { return authorId; }
    public void setAuthorId(UUID authorId)   { this.authorId = authorId; }
    public String getBody()                  { return body; }
    public void setBody(String body)         { this.body = body; }
    public Instant getCreatedAt()            { return createdAt; }
}
