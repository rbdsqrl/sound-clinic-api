package com.simplehearing.data.page;

import java.util.Optional;

/**
 * Data-layer contract for page persistence operations.
 *
 * This interface separates the application from the JPA implementation.
 */
public interface PageDataPort {

    Optional<PageEntity> findByPageId(String pageId);

    PageEntity save(PageEntity page);
}
