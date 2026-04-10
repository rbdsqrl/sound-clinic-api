package com.simplehearing.dto.page;

/**
 * A call-to-action button used across multiple section types.
 * {@code link} is a page-relative path (e.g. "/appointment") that clients
 * resolve against their own routing scheme.
 */
public record CtaButton(String label, String link) {}
