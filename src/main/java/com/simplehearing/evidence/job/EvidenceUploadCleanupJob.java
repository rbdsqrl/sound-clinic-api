package com.simplehearing.evidence.job;

import com.simplehearing.evidence.entity.EvidenceUpload;
import com.simplehearing.evidence.repository.EvidenceUploadRepository;
import com.simplehearing.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * A direct upload that was started but never completed (the phone lost signal for good, the app was closed…)
 * leaves a record and possibly a partial or unclaimed object in storage. An hour after its link expired it is
 * clearly abandoned — remove both so storage doesn't fill with orphaned videos.
 */
@Component
public class EvidenceUploadCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(EvidenceUploadCleanupJob.class);

    private final EvidenceUploadRepository uploadRepository;
    private final StorageService storageService;

    public EvidenceUploadCleanupJob(EvidenceUploadRepository uploadRepository, StorageService storageService) {
        this.uploadRepository = uploadRepository;
        this.storageService = storageService;
    }

    @Scheduled(cron = "0 15 * * * *")
    @Transactional
    public void sweep() {
        sweepOlderThan(Instant.now().minus(Duration.ofHours(1)));
    }

    /** Removes uploads whose link expired before {@code cutoff}; returns how many. */
    @Transactional
    public int sweepOlderThan(Instant cutoff) {
        List<EvidenceUpload> abandoned = uploadRepository.findByExpiresAtBefore(cutoff);
        if (abandoned.isEmpty()) return 0;
        abandoned.forEach(u -> storageService.delete(u.getStoredUrl()));
        uploadRepository.deleteAll(abandoned);
        log.info("Removed {} abandoned evidence upload(s)", abandoned.size());
        return abandoned.size();
    }
}
