package com.simplehearing.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "s3")
public class S3StorageService implements StorageService {

    private final S3Client s3;
    private final String bucket;
    private final String region;
    private final String publicUrlBase;

    public S3StorageService(StorageProperties props) {
        StorageProperties.S3Props p = props.getS3();
        this.bucket       = p.getBucket();
        this.region       = p.getRegion();
        this.publicUrlBase = p.getPublicUrlBase();

        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(p.getAccessKeyId(), p.getSecretAccessKey())));

        // Custom endpoint — required for Supabase, MinIO, etc.
        if (p.getEndpoint() != null && !p.getEndpoint().isBlank()) {
            builder.endpointOverride(URI.create(p.getEndpoint()));
        }

        // Path-style access — required for Supabase (uses /<bucket>/<key> not <bucket>.host/<key>)
        if (p.isForcePathStyle()) {
            builder.serviceConfiguration(
                    S3Configuration.builder().pathStyleAccessEnabled(true).build());
        }

        this.s3 = builder.build();
    }

    @Override
    public String store(MultipartFile file, String folder) throws IOException {
        String original = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "file");
        String safe = original.replaceAll("[^a-zA-Z0-9._-]", "_");
        String key  = folder + "/" + UUID.randomUUID() + "-" + safe;

        s3.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(file.getContentType())
                        .contentLength(file.getSize())
                        .build(),
                RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        return buildPublicUrl(key);
    }

    @Override
    public void delete(String fileUrl) {
        if (fileUrl == null) return;
        String base = urlPrefix();
        if (fileUrl.startsWith(base)) {
            String key = fileUrl.substring(base.length());
            try {
                s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
            } catch (Exception ignored) {}
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String buildPublicUrl(String key) {
        String base = urlPrefix();
        return base + key;
    }

    private String urlPrefix() {
        if (publicUrlBase != null && !publicUrlBase.isBlank()) {
            return publicUrlBase.endsWith("/") ? publicUrlBase : publicUrlBase + "/";
        }
        return "https://" + bucket + ".s3." + region + ".amazonaws.com/";
    }
}
