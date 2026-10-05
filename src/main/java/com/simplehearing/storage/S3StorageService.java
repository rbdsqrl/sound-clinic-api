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
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.OptionalLong;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "s3")
public class S3StorageService implements StorageService {

    private static final String SUPABASE_S3_ENDPOINT_SUFFIX = "/storage/v1/s3";

    private final S3Client s3;
    private final S3Presigner presigner;
    private final String bucket;
    private final String region;
    private final String endpoint;
    private final String publicUrlBase;

    public S3StorageService(StorageProperties props) {
        StorageProperties.S3Props p = props.getS3();
        this.bucket        = p.getBucket();
        this.region        = p.getRegion();
        this.endpoint      = p.getEndpoint();
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
    public String store(byte[] data, String filename, String contentType, String folder) throws IOException {
        String safe = StringUtils.cleanPath(filename).replaceAll("[^a-zA-Z0-9._-]", "_");
        String key  = folder + "/" + UUID.randomUUID() + "-" + safe;

        s3.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(contentType)
                        .contentLength((long) data.length)
                        .build(),
                RequestBody.fromBytes(data));

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

    @Override
    public boolean isHostedFile(String url) {
        return url != null && url.startsWith(urlPrefix());
    }

    /** A presigned PUT: content type and exact length are part of the signature, so the client can't send
     *  a different kind of file or a larger one than was approved. */
    @Override
    public DirectUpload prepareDirectUpload(String folder, String filename, String contentType,
                                            long sizeBytes, Duration validFor) {
        String safe = StringUtils.cleanPath(filename).replaceAll("[^a-zA-Z0-9._-]", "_");
        String key = folder + "/" + UUID.randomUUID() + "-" + safe;
        var presigned = presigner.presignPutObject(PutObjectPresignRequest.builder()
                .signatureDuration(validFor)
                .putObjectRequest(PutObjectRequest.builder()
                        .bucket(bucket).key(key).contentType(contentType).contentLength(sizeBytes).build())
                .build());
        return new DirectUpload(presigned.url().toString(), buildPublicUrl(key), Map.of("Content-Type", contentType));
    }

    @Override
    public OptionalLong storedSize(String storedUrl) {
        String prefix = urlPrefix();
        if (storedUrl == null || !storedUrl.startsWith(prefix)) return OptionalLong.empty();
        try {
            return OptionalLong.of(s3.headObject(HeadObjectRequest.builder()
                    .bucket(bucket).key(storedUrl.substring(prefix.length())).build()).contentLength());
        } catch (NoSuchKeyException e) {
            return OptionalLong.empty();
        } catch (software.amazon.awssdk.services.s3.model.S3Exception e) {
            if (e.statusCode() == 404) return OptionalLong.empty();
            throw e;
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
        String supabaseBase = deriveSupabasePublicUrlBase();
        if (supabaseBase != null) {
            return supabaseBase;
        }
        return "https://" + bucket + ".s3." + region + ".amazonaws.com/";
    }

    /** Supabase's S3-compatible API endpoint (".../storage/v1/s3") and its public object URL
     *  (".../storage/v1/object/public/{bucket}") share the same project host, just a different
     *  path suffix — so when the endpoint matches that shape, the public URL base can be derived
     *  from it instead of requiring STORAGE_S3_PUBLIC_URL_BASE to be set separately. Explicit
     *  config (checked above) still wins, e.g. for a custom storage domain in front of the bucket.
     *  Returns null for anything that isn't recognizably a Supabase endpoint (native AWS, MinIO,
     *  etc.), where this shortcut doesn't apply. */
    private String deriveSupabasePublicUrlBase() {
        if (endpoint == null || endpoint.isBlank() || bucket == null) return null;
        String trimmed = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
        if (!trimmed.endsWith(SUPABASE_S3_ENDPOINT_SUFFIX)) return null;
        String projectHost = trimmed.substring(0, trimmed.length() - SUPABASE_S3_ENDPOINT_SUFFIX.length());
        return projectHost + "/storage/v1/object/public/" + bucket + "/";
    }
}
