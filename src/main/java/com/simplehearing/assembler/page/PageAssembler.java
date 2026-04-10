package com.simplehearing.assembler.page;

import com.simplehearing.dto.page.PageResponse;

/**
 * Contract for page assemblers.
 *
 * Each page (home, services, appointment, …) has its own assembler
 * implementation. The assembler is responsible for gathering all data
 * needed to build its {@link PageResponse} — currently from stub data,
 * later from injected repositories or services.
 *
 * Assemblers are registered as Spring beans named after their {@code pageId}
 * (e.g. {@code @Component("home")}). {@link com.simplehearing.service.page.PageServiceImpl}
 * resolves them automatically by name.
 */
public interface PageAssembler {
    PageResponse assemble();
}
