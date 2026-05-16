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

        public String getBucket()                        { return bucket; }
        public void setBucket(String bucket)             { this.bucket = bucket; }

        public String getRegion()                        { return region; }
        public void setRegion(String region)             { this.region = region; }

        public String getAccessKeyId()                   { return accessKeyId; }
        public void setAccessKeyId(String v)             { this.accessKeyId = v; }

        public String getSecretAccessKey()               { return secretAccessKey; }
        public void setSecretAccessKey(String v)         { this.secretAccessKey = v; }
    }
}
