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
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "s3")
public class S3StorageService implements StorageService {

    private final S3Client s3;
    private final String bucket;
    private final String region;

    public S3StorageService(StorageProperties props) {
        StorageProperties.S3Props s3Props = props.getS3();
        this.bucket = s3Props.getBucket();
        this.region = s3Props.getRegion();
        this.s3 = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(s3Props.getAccessKeyId(), s3Props.getSecretAccessKey())))
                .build();
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

        return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
    }

    @Override
    public void delete(String fileUrl) {
        String prefix = "https://" + bucket + ".s3." + region + ".amazonaws.com/";
        if (fileUrl != null && fileUrl.startsWith(prefix)) {
            String key = fileUrl.substring(prefix.length());
            try {
                s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
            } catch (Exception ignored) {}
        }
    }
}
