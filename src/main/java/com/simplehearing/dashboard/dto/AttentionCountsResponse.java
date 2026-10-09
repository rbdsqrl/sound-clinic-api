package com.simplehearing.dashboard.dto;

/**
 * How many items are waiting on each of the dashboard's "needs attention" cards. The dashboard
 * asks for these first and fetches a card's full list only when its count is above zero.
 */
public record AttentionCountsResponse(
        int pendingReschedule,
        int cancellationRequests,
        int openConcerns
) {}
