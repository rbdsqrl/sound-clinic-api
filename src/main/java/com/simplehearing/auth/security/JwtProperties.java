package com.simplehearing.auth.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /** Base64-encoded HMAC-SHA256 secret key (minimum 256-bit). */
    private String secret;

    /** Access token lifetime in seconds (default: 15 min). */
    private long accessTokenExpiry = 900;

    /** Refresh token lifetime in seconds (default: 7 days). */
    private long refreshTokenExpiry = 604800;

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }

    public long getAccessTokenExpiry() { return accessTokenExpiry; }
    public void setAccessTokenExpiry(long accessTokenExpiry) { this.accessTokenExpiry = accessTokenExpiry; }

    public long getRefreshTokenExpiry() { return refreshTokenExpiry; }
    public void setRefreshTokenExpiry(long refreshTokenExpiry) { this.refreshTokenExpiry = refreshTokenExpiry; }
}
