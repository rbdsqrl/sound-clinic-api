package com.simplehearing.feed.dto;

import com.simplehearing.feed.entity.FeedPost;
import com.simplehearing.feed.enums.FeedPostType;
import com.simplehearing.user.entity.User;
import com.simplehearing.user.enums.Role;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FeedPostResponse(
        UUID id,
        UUID orgId,
        UUID authorId,
        String authorFirstName,
        String authorLastName,
        Role authorRole,
        String title,
        String body,
        FeedPostType type,
        /** Named recipients — empty means visible to everyone in the org. */
        List<RecipientSummary> recipients,
        /** Staff who attended the meeting — only meaningful for MOM. */
        List<RecipientSummary> attendees,
        Instant createdAt,
        Instant updatedAt,
        long likeCount,
        boolean likedByMe,
        long viewCount,
        long commentCount,
        List<FeedPostImageResponse> images
) {
    public record RecipientSummary(UUID id, String firstName, String lastName) {}

    public static FeedPostResponse from(
            FeedPost post, User author,
            long likeCount, boolean likedByMe, long viewCount, long commentCount,
            List<FeedPostImageResponse> images, List<RecipientSummary> recipients,
            List<RecipientSummary> attendees) {
        return new FeedPostResponse(
                post.getId(),
                post.getOrgId(),
                post.getAuthorId(),
                author != null ? author.getFirstName() : null,
                author != null ? author.getLastName() : null,
                author != null ? author.getRole() : null,
                post.getTitle(),
                post.getBody(),
                post.getType(),
                recipients,
                attendees,
                post.getCreatedAt(),
                post.getUpdatedAt(),
                likeCount,
                likedByMe,
                viewCount,
                commentCount,
                images
        );
    }
}
