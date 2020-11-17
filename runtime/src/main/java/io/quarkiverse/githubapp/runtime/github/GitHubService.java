package io.quarkiverse.githubapp.runtime.github;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.kohsuke.github.GHAppInstallationToken;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.LoadingCache;

import io.quarkiverse.githubapp.runtime.config.GitHubAppRuntimeConfig;
import io.quarkiverse.githubapp.runtime.signing.JwtTokenCreator;

@Singleton
public class GitHubService {

    private final GitHubAppRuntimeConfig gitHubAppRuntimeConfig;

    private final JwtTokenCreator jwtTokenCreator;

    private final LoadingCache<Long, CachedInstallationGitHub> installationCache;

    @Inject
    public GitHubService(GitHubAppRuntimeConfig gitHubAppRuntimeConfig, JwtTokenCreator jwtTokenCreator) {
        this.gitHubAppRuntimeConfig = gitHubAppRuntimeConfig;
        this.jwtTokenCreator = jwtTokenCreator;
        this.installationCache = Caffeine.newBuilder()
                .expireAfter(new Expiry<Long, CachedInstallationGitHub>() {
                    @Override
                    public long expireAfterCreate(Long installationId, CachedInstallationGitHub cachedInstallationGitHub,
                            long currentTime) {
                        long millis = cachedInstallationGitHub.getExpiration()
                                .minus(System.currentTimeMillis(), ChronoUnit.MILLIS)
                                .minus(10, ChronoUnit.MINUTES)
                                .toEpochMilli();
                        return TimeUnit.MILLISECONDS.toNanos(millis);
                    }

                    @Override
                    public long expireAfterUpdate(Long installationId, CachedInstallationGitHub cachedInstallationGitHub,
                            long currentTime, long currentDuration) {
                        // TODO, should we implement that too?
                        return currentDuration;
                    }

                    @Override
                    public long expireAfterRead(Long installationId, CachedInstallationGitHub cachedInstallationGitHub,
                            long currentTime, long currentDuration) {
                        return currentDuration;
                    }
                })
                .build(this::createInstallationGitHub);
    }

    public GitHub getInstallationClient(Long installationId) {
        return installationCache.get(installationId).getGitHub();
    }

    @SuppressWarnings("deprecation")
    private CachedInstallationGitHub createInstallationGitHub(Long installationId) {
        try {
            GHAppInstallationToken installationToken = createApplicationGitHub().getApp().getInstallationById(installationId)
                    .createToken().create();
            return new CachedInstallationGitHub(
                    new GitHubBuilder().withAppInstallationToken(installationToken.getToken()).build(),
                    installationToken.getExpiresAt());
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create a GitHub client for the installation", e);
        }
    }

    // TODO even if we have a cache for the other one, we should probably also keep this one around for a few minutes
    private GitHub createApplicationGitHub() {
        String jwtToken;

        try {
            // maximum TTL is 10 minutes
            jwtToken = jwtTokenCreator.createJwtToken(gitHubAppRuntimeConfig.appId, gitHubAppRuntimeConfig.privateKey, 540_000);
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalStateException("Unable to generate the JWT token", e);
        }

        try {
            return new GitHubBuilder().withJwtToken(jwtToken).build();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create a GitHub client for the application", e);
        }
    }
}
