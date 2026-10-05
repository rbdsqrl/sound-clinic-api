package com.simplehearing.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.OptionalLong;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

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
    public String store(byte[] data, String filename, String contentType, String folder) throws IOException {
        String safe = StringUtils.cleanPath(filename).replaceAll("[^a-zA-Z0-9._-]", "_");
        String stored = UUID.randomUUID() + "-" + safe;

        Path targetDir = baseDir.resolve(folder);
        Files.createDirectories(targetDir);
        Files.write(targetDir.resolve(stored), data);

        return baseUrl + "/api/v1/files/" + folder + "/" + stored;
    }

    @Override
    public String presign(String storedUrl, Duration duration) {
        return storedUrl;
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

    @Override
    public boolean isHostedFile(String url) {
        return url != null && url.startsWith(baseUrl + "/api/v1/files/");
    }

    // ── Direct uploads (dev) ─────────────────────────────────────────────────
    // Mirrors the S3 flow: a signed, expiring token authorises exactly one file of one size and type. The
    // signing key is random per start, so tokens simply stop working after a restart — fine for local dev.

    private final byte[] tokenKey = randomKey();

    private static byte[] randomKey() {
        byte[] k = new byte[32];
        new SecureRandom().nextBytes(k);
        return k;
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(tokenKey, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public DirectUpload prepareDirectUpload(String folder, String filename, String contentType,
                                            long sizeBytes, Duration validFor) {
        String safe = StringUtils.cleanPath(filename).replaceAll("[^a-zA-Z0-9._-]", "_");
        String rel = folder + "/" + UUID.randomUUID() + "-" + safe;
        String payload = rel + "|" + contentType + "|" + sizeBytes + "|" + Instant.now().plus(validFor).getEpochSecond();
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8))
                + "." + sign(payload);
        return new DirectUpload(baseUrl + "/api/v1/files/direct/" + token,
                baseUrl + "/api/v1/files/" + rel, Map.of("Content-Type", contentType));
    }

    /** Writes an upload authorised by {@link #prepareDirectUpload}; throws {@link SecurityException} for a bad,
     *  expired or mismatching request. */
    public void acceptDirectUpload(String token, InputStream body, long contentLength, String contentType) throws IOException {
        String[] parts = token.split("\\.", 2);
        if (parts.length != 2) throw new SecurityException("Malformed upload token");
        String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(sign(payload).getBytes(StandardCharsets.UTF_8), parts[1].getBytes(StandardCharsets.UTF_8))) {
            throw new SecurityException("Invalid upload token");
        }
        String[] f = payload.split("\\|");
        String rel = f[0], type = f[1];
        long size = Long.parseLong(f[2]), expires = Long.parseLong(f[3]);
        if (Instant.now().getEpochSecond() > expires) throw new SecurityException("Upload link has expired");
        if (contentLength != size || contentType == null || !contentType.startsWith(type.split(";")[0])) {
            throw new SecurityException("The file doesn't match what was approved");
        }
        Path target = baseDir.resolve(rel).normalize();
        if (!target.startsWith(baseDir)) throw new SecurityException("Bad path");
        Files.createDirectories(target.getParent());
        long written = 0;
        try (var out = Files.newOutputStream(target)) {
            byte[] buf = new byte[64 * 1024];
            int n;
            while ((n = body.read(buf)) > 0) {
                written += n;
                if (written > size) { out.close(); Files.deleteIfExists(target); throw new SecurityException("More data than approved"); }
                out.write(buf, 0, n);
            }
        }
    }

    @Override
    public OptionalLong storedSize(String storedUrl) {
        String prefix = baseUrl + "/api/v1/files/";
        if (storedUrl == null || !storedUrl.startsWith(prefix)) return OptionalLong.empty();
        try {
            Path p = baseDir.resolve(storedUrl.substring(prefix.length())).normalize();
            return p.startsWith(baseDir) && Files.exists(p) ? OptionalLong.of(Files.size(p)) : OptionalLong.empty();
        } catch (IOException e) {
            return OptionalLong.empty();
        }
    }

    public Path getBaseDir() { return baseDir; }
}
