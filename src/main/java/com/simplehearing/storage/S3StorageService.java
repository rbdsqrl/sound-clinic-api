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
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "s3")
public class S3StorageService implements StorageService {

    private final S3Client s3;
    private final S3Presigner presigner;
    private final String bucket;
    private final String region;
    private final String publicUrlBase;

    public S3StorageService(StorageProperties props) {
        StorageProperties.S3Props p = props.getS3();
        this.bucket        = p.getBucket();
        this.region        = p.getRegion();
        this.publicUrlBase = p.getPublicUrlBase();

        StaticCredentialsProvider creds = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(p.getAccessKeyId(), p.getSecretAccessKey()));
        S3Configuration s3Config = S3Configuration.builder()
                .pathStyleAccessEnabled(p.isForcePathStyle())
                .build();

        S3ClientBuilder clientBuilder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(creds)
                .serviceConfiguration(s3Config);

        S3Presigner.Builder presignerBuilder = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(creds)
                .serviceConfiguration(s3Config);

        if (p.getEndpoint() != null && !p.getEndpoint().isBlank()) {
            URI endpoint = URI.create(p.getEndpoint());
            clientBuilder.endpointOverride(endpoint);
            presignerBuilder.endpointOverride(endpoint);
        }

        this.s3       = clientBuilder.build();
        this.presigner = presignerBuilder.build();
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

    @Override
    public String presign(String storedUrl, Duration duration) {
        String prefix = urlPrefix();
        if (storedUrl == null || !storedUrl.startsWith(prefix)) return storedUrl;
        String key = storedUrl.substring(prefix.length());
        return presigner.presignGetObject(GetObjectPresignRequest.builder()
                .signatureDuration(duration)
                .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(key).build())
                .build())
                .url().toString();
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
