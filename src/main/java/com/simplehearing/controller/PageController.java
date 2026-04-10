package com.simplehearing.controller;

import com.simplehearing.dto.page.PageResponse;
import com.simplehearing.service.page.PageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves fully-assembled page layouts for any registered page.
 *
 * <pre>
 * GET /page/{pageId}
 * </pre>
 *
 * <p>Returns a {@link PageResponse} containing the page metadata and an ordered
 * list of sections, each carrying a typed {@code data} payload.
 *
 * <p>Responds with {@code 404 Not Found} when no assembler is registered for
 * the requested {@code pageId}.
 *
 * <h3>Registered pages</h3>
 * <ul>
 *   <li>{@code GET /page/home} — home page layout</li>
 * </ul>
 */
@RestController
@RequestMapping("/page")
public class PageController {

    private final PageService pageService;

    public PageController(PageService pageService) {
        this.pageService = pageService;
    }

    @GetMapping("/{pageId}")
    public ResponseEntity<PageResponse> getPage(@PathVariable String pageId) {
        return pageService.getPage(pageId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
