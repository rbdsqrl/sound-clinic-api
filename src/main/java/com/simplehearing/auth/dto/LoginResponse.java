package com.simplehearing.auth.dto;

import com.simplehearing.user.dto.UserResponse;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        UserResponse user
) {}
