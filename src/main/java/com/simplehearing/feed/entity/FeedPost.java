package com.simplehearing.feed.entity;

import com.simplehearing.feed.enums.FeedPostType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "feed_posts")
public class FeedPost {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FeedPostType type = FeedPostType.POST;

    /** Empty means visible to everyone in the org (the default) — same convention as an
     *  {@code AssignActivityRequest} with no therapist: absence means "no restriction". */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "feed_post_recipients", joinColumns = @JoinColumn(name = "post_id"))
    @Column(name = "user_id")
    private Set<UUID> recipientIds = new HashSet<>();

    /** Staff who attended the meeting — meaningful for MOM only, never used for access control. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "feed_post_attendees", joinColumns = @JoinColumn(name = "post_id"))
    @Column(name = "user_id")
    private Set<UUID> attendeeIds = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public FeedPost() {}

    public UUID getId() { return id; }
    public UUID getOrgId() { return orgId; }
    public void setOrgId(UUID orgId) { this.orgId = orgId; }
    public UUID getAuthorId() { return authorId; }
    public void setAuthorId(UUID authorId) { this.authorId = authorId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public FeedPostType getType() { return type; }
    public void setType(FeedPostType type) { this.type = type; }
    public Set<UUID> getRecipientIds() { return recipientIds; }
    public void setRecipientIds(Set<UUID> recipientIds) { this.recipientIds = recipientIds; }
    public Set<UUID> getAttendeeIds() { return attendeeIds; }
    public void setAttendeeIds(Set<UUID> attendeeIds) { this.attendeeIds = attendeeIds; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
