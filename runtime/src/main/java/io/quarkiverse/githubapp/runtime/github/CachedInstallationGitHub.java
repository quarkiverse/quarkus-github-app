package io.quarkiverse.githubapp.runtime.github;

import java.time.Instant;
import java.util.Date;

import org.kohsuke.github.GitHub;

class CachedInstallationGitHub {

    private final GitHub gitHub;

    private final Instant expiration;

    CachedInstallationGitHub(GitHub gitHub, Date expirationDate) {
        this.gitHub = gitHub;
        this.expiration = expirationDate.toInstant();
    }

    public GitHub getGitHub() {
        return gitHub;
    }

    public Instant getExpiration() {
        return expiration;
    }
}
