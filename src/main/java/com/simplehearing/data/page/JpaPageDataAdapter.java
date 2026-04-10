package com.simplehearing.data.page;

import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JpaPageDataAdapter implements PageDataPort {

    private final PageJpaRepository repository;

    public JpaPageDataAdapter(PageJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<PageEntity> findByPageId(String pageId) {
        return repository.findByPageId(pageId);
    }

    @Override
    public PageEntity save(PageEntity page) {
        return repository.save(page);
    }
}
