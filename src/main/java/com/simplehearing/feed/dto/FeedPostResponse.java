package com.simplehearing.feed.dto;

import com.simplehearing.feed.entity.FeedPost;
import com.simplehearing.user.entity.User;
import com.simplehearing.user.enums.Role;

import java.time.Instant;
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
        Instant createdAt,
        Instant updatedAt
) {
    public static FeedPostResponse from(FeedPost post, User author) {
        return new FeedPostResponse(
                post.getId(),
                post.getOrgId(),
                post.getAuthorId(),
                author != null ? author.getFirstName() : null,
                author != null ? author.getLastName() : null,
                author != null ? author.getRole() : null,
                post.getTitle(),
                post.getBody(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}
