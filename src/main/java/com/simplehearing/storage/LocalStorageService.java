package com.simplehearing.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {

    private final Path baseDir;
    private final String baseUrl;

    public LocalStorageService(StorageProperties props) {
        this.baseDir = Paths.get(props.getLocal().getBaseDir()).toAbsolutePath().normalize();
        this.baseUrl = props.getBaseUrl();
        try {
            Files.createDirectories(this.baseDir);
        } catch (IOException e) {
            throw new RuntimeException("Cannot create upload directory: " + this.baseDir, e);
        }
    }

    @Override
    public String store(MultipartFile file, String folder) throws IOException {
        String original = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "file");
        String safe = original.replaceAll("[^a-zA-Z0-9._-]", "_");
        String filename = UUID.randomUUID() + "-" + safe;

        Path targetDir = baseDir.resolve(folder);
        Files.createDirectories(targetDir);
        Files.copy(file.getInputStream(), targetDir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);

        return baseUrl + "/api/v1/files/" + folder + "/" + filename;
    }

    @Override
    public void delete(String fileUrl) {
        String prefix = baseUrl + "/api/v1/files/";
        if (fileUrl != null && fileUrl.startsWith(prefix)) {
            String rel = fileUrl.substring(prefix.length());
            try {
                Files.deleteIfExists(baseDir.resolve(rel).normalize());
            } catch (IOException ignored) {}
        }
    }

    public Path getBaseDir() { return baseDir; }
}
