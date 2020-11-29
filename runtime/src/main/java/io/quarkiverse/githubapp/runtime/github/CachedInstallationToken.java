package io.quarkiverse.githubapp.runtime.github;

import java.time.Instant;
import java.util.Date;

class CachedInstallationToken {

    private final String token;

    private final Instant expiresAt;

    CachedInstallationToken(String token, Date expiresAt) {
        this.token = token;
        this.expiresAt = expiresAt.toInstant();
    }

    public String getToken() {
        return token;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
