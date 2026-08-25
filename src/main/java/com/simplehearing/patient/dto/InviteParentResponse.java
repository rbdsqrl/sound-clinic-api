package com.simplehearing.patient.dto;

/** The accept-invite path, e.g. "/accept-invite?token=...". Share this with the parent. */
public record InviteParentResponse(String inviteLink) {}
