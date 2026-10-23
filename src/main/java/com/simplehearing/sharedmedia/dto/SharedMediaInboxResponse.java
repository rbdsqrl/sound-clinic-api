package com.simplehearing.sharedmedia.dto;

import java.util.List;

/** The dashboard's notes card: how many are unread, plus the unread ones first and then the latest read ones. */
public record SharedMediaInboxResponse(int unreadCount, List<SharedMediaResponse> items) {}
