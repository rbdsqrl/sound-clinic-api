package com.simplehearing.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * @param identifier Either the account's email address or its phone number — AuthService tells
 *                   them apart (an email always contains '@', a phone number never does) and
 *                   normalises/looks up accordingly. Left unnormalised here since which
 *                   normaliser applies depends on that check.
 */
public record LoginRequest(
        @NotBlank String identifier,
        @NotBlank String password
) {}
