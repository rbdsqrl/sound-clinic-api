package com.simplehearing.service;

import com.simplehearing.dto.response.GalleryItemResponse;
import com.simplehearing.entity.GalleryItem;
import com.simplehearing.enums.GalleryType;
import com.simplehearing.exception.ResourceNotFoundException;
import com.simplehearing.repository.GalleryItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class GalleryService {

    private final GalleryItemRepository repo;

    public GalleryService(GalleryItemRepository repo) {
        this.repo = repo;
    }

    public List<GalleryItemResponse> getPhotos() {
        return repo.findByActiveTrueAndTypeOrderByDisplayOrderAsc(GalleryType.PHOTO)
            .stream().map(GalleryItemResponse::from).toList();
    }

    public List<GalleryItemResponse> getVideos() {
        return repo.findByActiveTrueAndTypeOrderByDisplayOrderAsc(GalleryType.VIDEO)
            .stream().map(GalleryItemResponse::from).toList();
    }

    public GalleryItemResponse getById(Long id) {
        return repo.findById(id)
            .filter(GalleryItem::isActive)
            .map(GalleryItemResponse::from)
            .orElseThrow(() -> new ResourceNotFoundException("Gallery item", id));
    }

    @Transactional
    public GalleryItemResponse create(GalleryItem item) {
        return GalleryItemResponse.from(repo.save(item));
    }

    @Transactional
    public GalleryItemResponse update(Long id, GalleryItem updated) {
        GalleryItem existing = repo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Gallery item", id));
        existing.setTitle(updated.getTitle());
        existing.setCategory(updated.getCategory());
        existing.setMediaUrl(updated.getMediaUrl());
        existing.setThumbnailUrl(updated.getThumbnailUrl());
        existing.setDuration(updated.getDuration());
        existing.setColorHex(updated.getColorHex());
        existing.setDisplayOrder(updated.getDisplayOrder());
        return GalleryItemResponse.from(repo.save(existing));
    }

    @Transactional
    public void delete(Long id) {
        GalleryItem item = repo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Gallery item", id));
        item.setActive(false);
        repo.save(item);
    }
}
