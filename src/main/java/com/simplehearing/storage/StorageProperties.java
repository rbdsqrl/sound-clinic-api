package com.simplehearing.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    /** Active provider: "local" (default) or "s3" */
    private String provider = "local";

    /** Base URL of this server — used to build local file URLs */
    private String baseUrl = "http://localhost:8080";

    private LocalProps local = new LocalProps();
    private S3Props s3 = new S3Props();

    public String getProvider()  { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getBaseUrl()   { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public LocalProps getLocal() { return local; }
    public void setLocal(LocalProps local) { this.local = local; }

    public S3Props getS3()       { return s3; }
    public void setS3(S3Props s3) { this.s3 = s3; }

    public static class LocalProps {
        private String baseDir = System.getProperty("user.home") + "/.simplehearing/uploads";

        public String getBaseDir()             { return baseDir; }
        public void setBaseDir(String baseDir) { this.baseDir = baseDir; }
    }

    public static class S3Props {
        private String bucket;
        private String region = "ap-south-1";
        private String accessKeyId;
        private String secretAccessKey;
        /** Custom S3-compatible endpoint (e.g. Supabase, MinIO). Omit for native AWS. */
        private String endpoint;
        /** Required for Supabase and MinIO — they use path-style URLs, not virtual-hosted. */
        private boolean forcePathStyle = false;
        /**
         * Base URL used to build the public access URL for stored files.
         * For Supabase: https://&lt;project&gt;.supabase.co/storage/v1/object/public/&lt;bucket&gt;
         * Leave blank to auto-generate a standard AWS S3 URL.
         */
        private String publicUrlBase;

        public String getBucket()                        { return bucket; }
        public void setBucket(String bucket)             { this.bucket = bucket; }

        public String getRegion()                        { return region; }
        public void setRegion(String region)             { this.region = region; }

        public String getAccessKeyId()                   { return accessKeyId; }
        public void setAccessKeyId(String v)             { this.accessKeyId = v; }

        public String getSecretAccessKey()               { return secretAccessKey; }
        public void setSecretAccessKey(String v)         { this.secretAccessKey = v; }

        public String getEndpoint()                      { return endpoint; }
        public void setEndpoint(String endpoint)         { this.endpoint = endpoint; }

        public boolean isForcePathStyle()                { return forcePathStyle; }
        public void setForcePathStyle(boolean v)         { this.forcePathStyle = v; }

        public String getPublicUrlBase()                 { return publicUrlBase; }
        public void setPublicUrlBase(String v)           { this.publicUrlBase = v; }
    }
}
