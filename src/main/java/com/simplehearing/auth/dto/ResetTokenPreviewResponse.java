package com.simplehearing.auth.dto;

/**
 * Shown on the reset page so the user can confirm which account they are changing.
 * The email is masked — the link holder already knows the address, nobody else learns it.
 */
public record ResetTokenPreviewResponse(String maskedEmail) {}
