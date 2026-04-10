package com.simplehearing.service.page;

import com.simplehearing.dto.page.PageResponse;

import java.util.Optional;

/**
 * Contract for retrieving fully-assembled page layouts.
 *
 * The service accepts a {@code pageId} (e.g. {@code "home"}, {@code "services"})
 * and returns the corresponding {@link PageResponse}, or {@link Optional#empty()}
 * when no assembler is registered for that page.
 */
public interface PageService {

    /**
     * Returns the assembled layout for the given page, or empty if the page
     * is not registered.
     *
     * @param pageId the page identifier (case-sensitive, matches the Spring bean name)
     */
    Optional<PageResponse> getPage(String pageId);
}
