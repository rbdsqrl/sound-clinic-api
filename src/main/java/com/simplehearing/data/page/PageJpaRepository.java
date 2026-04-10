package com.simplehearing.data.page;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PageJpaRepository extends JpaRepository<PageEntity, Long> {
    Optional<PageEntity> findByPageId(String pageId);
}
