package com.simplehearing.repository;

import com.simplehearing.entity.GalleryItem;
import com.simplehearing.enums.GalleryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GalleryItemRepository extends JpaRepository<GalleryItem, Long> {
    List<GalleryItem> findByActiveTrueAndTypeOrderByDisplayOrderAsc(GalleryType type);
    List<GalleryItem> findByActiveTrueOrderByDisplayOrderAsc();
}
