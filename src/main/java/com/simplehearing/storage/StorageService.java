package com.simplehearing.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Provider-agnostic file storage abstraction.
 * Implementations: LocalStorageService (dev), S3StorageService (prod).
 * Switch via app.storage.provider = local | s3
 */
public interface StorageService {

    /**
     * Stores a file and returns its publicly accessible URL.
     *
     * @param file   the uploaded file
     * @param folder logical folder prefix, e.g. "sessions/uuid"
     * @return the URL at which the file can be retrieved
     */
    String store(MultipartFile file, String folder) throws IOException;

    /** Deletes the file at the given URL. Best-effort — no exception if not found. */
    void delete(String fileUrl);
}
