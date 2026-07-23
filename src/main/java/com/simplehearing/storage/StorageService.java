package com.simplehearing.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;

/**
 * Provider-agnostic file storage abstraction.
 * Implementations: LocalStorageService (dev), S3StorageService (prod).
 * Switch via app.storage.provider = local | s3
 */
public interface StorageService {

    /**
     * Stores a file and returns its canonical stored URL (used as the DB reference).
     *
     * @param file   the uploaded file
     * @param folder logical folder prefix, e.g. "sessions/uuid"
     * @return the canonical URL stored in the database
     */
    String store(MultipartFile file, String folder) throws IOException;

    /**
     * Returns a URL that can be used to download the file.
     * For S3: a time-limited presigned URL. For local: the stored URL as-is.
     *
     * @param storedUrl the URL previously returned by {@link #store}
     * @param duration  how long the presigned URL should be valid
     */
    String presign(String storedUrl, Duration duration);

    /** Deletes the file at the given URL. Best-effort — no exception if not found. */
    void delete(String fileUrl);
}
