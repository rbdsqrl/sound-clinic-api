package com.simplehearing.invitation.dto;

import com.simplehearing.user.enums.Role;

public record InvitePreviewResponse(String email, Role role, String orgName) {}
