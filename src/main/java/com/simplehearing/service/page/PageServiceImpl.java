package com.simplehearing.service.page;

import com.simplehearing.assembler.page.PageAssembler;
import com.simplehearing.dto.page.PageResponse;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

/**
 * Resolves a {@link PageAssembler} by {@code pageId} and delegates assembly to it.
 *
 * <p>Spring automatically populates {@code assemblers} with every bean that
 * implements {@link PageAssembler}, keyed by the bean name. Because each
 * assembler is annotated {@code @Component("<pageId>")}, the map key equals
 * the page identifier — no explicit wiring is required when a new page is added.
 *
 * <h3>Adding a new page</h3>
 * <ol>
 *   <li>Create a class that implements {@link PageAssembler}.</li>
 *   <li>Annotate it with {@code @Component("yourPageId")}.</li>
 *   <li>That's it — this service picks it up automatically.</li>
 * </ol>
 */
@Service
public class PageServiceImpl implements PageService {

    private final Map<String, PageAssembler> assemblers;

    public PageServiceImpl(Map<String, PageAssembler> assemblers) {
        this.assemblers = assemblers;
    }

    @Override
    public Optional<PageResponse> getPage(String pageId) {
        PageAssembler assembler = assemblers.get(pageId);
        if (assembler == null) {
            return Optional.empty();
        }
        return Optional.of(assembler.assemble());
    }
}
