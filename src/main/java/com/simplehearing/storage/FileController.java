package com.simplehearing.storage;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Serves locally stored files at /api/v1/files/**.
 * Only active when storage.provider=local (the default).
 * In production (S3), files are served directly by S3 — this bean is not created.
 */
@RestController
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "local", matchIfMissing = true)
public class FileController {

    private final LocalStorageService storageService;

    public FileController(LocalStorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping("/api/v1/files/**")
    public ResponseEntity<Resource> serveFile(HttpServletRequest request) throws IOException {
        String rel = request.getRequestURI().replaceFirst("^/api/v1/files/", "");
        Path file = storageService.getBaseDir().resolve(rel).normalize();

        // Prevent path traversal
        if (!file.startsWith(storageService.getBaseDir())) {
            return ResponseEntity.badRequest().build();
        }

        Resource resource = new FileSystemResource(file);
        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.notFound().build();
        }

        String contentType = Files.probeContentType(file);
        if (contentType == null) contentType = "application/octet-stream";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }
}
