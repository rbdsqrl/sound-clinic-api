package com.simplehearing.feed.dto;

import java.util.Set;
import java.util.UUID;

public record UpdateFeedPostRequest(
        String title,
        String body,
        /** Null leaves the audience unchanged; an empty set explicitly clears it back to
         *  "everyone in the org". */
        Set<UUID> recipientIds,
        /** Same null/empty convention as recipientIds, for the MOM attendee list. */
        Set<UUID> attendeeIds
) {}
