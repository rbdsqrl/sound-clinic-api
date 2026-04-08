package com.simplehearing.controller;

import com.simplehearing.dto.response.GalleryItemResponse;
import com.simplehearing.entity.GalleryItem;
import com.simplehearing.service.GalleryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/gallery")
@Tag(name = "Gallery", description = "Photos and videos gallery")
public class GalleryController {

    private final GalleryService galleryService;

    public GalleryController(GalleryService galleryService) {
        this.galleryService = galleryService;
    }

    @GetMapping("/photos")
    @Operation(summary = "List all gallery photos")
    public ResponseEntity<List<GalleryItemResponse>> getPhotos() {
        return ResponseEntity.ok(galleryService.getPhotos());
    }

    @GetMapping("/videos")
    @Operation(summary = "List all gallery videos")
    public ResponseEntity<List<GalleryItemResponse>> getVideos() {
        return ResponseEntity.ok(galleryService.getVideos());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a gallery item by ID")
    public ResponseEntity<GalleryItemResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(galleryService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Add a gallery item (admin)")
    public ResponseEntity<GalleryItemResponse> create(@RequestBody GalleryItem item) {
        return ResponseEntity.status(HttpStatus.CREATED).body(galleryService.create(item));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a gallery item (admin)")
    public ResponseEntity<GalleryItemResponse> update(
        @PathVariable Long id, @RequestBody GalleryItem item) {
        return ResponseEntity.ok(galleryService.update(id, item));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Remove a gallery item (admin)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        galleryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
