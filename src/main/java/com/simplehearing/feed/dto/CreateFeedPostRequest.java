package com.simplehearing.feed.dto;

import com.simplehearing.feed.enums.FeedPostType;
import jakarta.validation.constraints.NotBlank;

import java.util.Set;
import java.util.UUID;

public record CreateFeedPostRequest(
        @NotBlank String title,
        String body,
        /** Defaults to POST when omitted. */
        FeedPostType type,
        /** Who this post is visible to, beyond its own author — omit or leave empty for
         *  "everyone in the org" (the default). Ignored when type is MOM, which is always
         *  staff-wide rather than targeted at specific people. */
        Set<UUID> recipientIds,
        /** Staff who attended the meeting — only meaningful when type is MOM, ignored otherwise. */
        Set<UUID> attendeeIds
) {}
