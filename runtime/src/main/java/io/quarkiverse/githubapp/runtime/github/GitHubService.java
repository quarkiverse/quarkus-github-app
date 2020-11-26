package io.quarkiverse.githubapp.runtime.github;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.kohsuke.github.GHAppInstallationToken;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.LoadingCache;

import io.quarkiverse.githubapp.runtime.config.GitHubAppRuntimeConfig;
import io.quarkiverse.githubapp.runtime.github.okhttp.OkHttpConnector;
import io.quarkiverse.githubapp.runtime.signing.JwtTokenCreator;
import okhttp3.OkHttpClient;

@ApplicationScoped
public class GitHubService {

    private final GitHubAppRuntimeConfig gitHubAppRuntimeConfig;

    private final JwtTokenCreator jwtTokenCreator;

    private final OkHttpConnector okhttpConnector;

    private final LoadingCache<Long, CachedInstallationToken> installationTokenCache;

    @Inject
    public GitHubService(GitHubAppRuntimeConfig gitHubAppRuntimeConfig, JwtTokenCreator jwtTokenCreator, OkHttpClient client) {
        this.gitHubAppRuntimeConfig = gitHubAppRuntimeConfig;
        this.jwtTokenCreator = jwtTokenCreator;
        this.okhttpConnector = new OkHttpConnector(client);
        this.installationTokenCache = Caffeine.newBuilder()
                .maximumSize(50)
                .expireAfter(new Expiry<Long, CachedInstallationToken>() {
                    @Override
                    public long expireAfterCreate(Long installationId, CachedInstallationToken cachedInstallationGitHub,
                            long currentTime) {
                        long millis = cachedInstallationGitHub.getExpiresAt()
                                .minus(System.currentTimeMillis(), ChronoUnit.MILLIS)
                                .minus(10, ChronoUnit.MINUTES)
                                .toEpochMilli();
                        return TimeUnit.MILLISECONDS.toNanos(millis);
                    }

                    @Override
                    public long expireAfterUpdate(Long installationId, CachedInstallationToken cachedInstallationGitHub,
                            long currentTime, long currentDuration) {
                        // TODO, should we implement that too?
                        return currentDuration;
                    }

                    @Override
                    public long expireAfterRead(Long installationId, CachedInstallationToken cachedInstallationGitHub,
                            long currentTime, long currentDuration) {
                        return currentDuration;
                    }
                })
                .build(new CreateInstallationToken());
    }

    public GitHub getInstallationClient(Long installationId) {
        CachedInstallationToken installationToken = installationTokenCache.get(installationId);

        try {
            return new GitHubBuilder().withConnector(okhttpConnector)
                    .withAppInstallationToken(installationToken.getToken()).build();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create a GitHub client for the installation " + installationId, e);
        }
    }

    // Using a lambda leads to a warning
    private class CreateInstallationToken implements CacheLoader<Long, CachedInstallationToken> {

        @SuppressWarnings("deprecation")
        @Override
        public CachedInstallationToken load(Long installationId) throws Exception {
            try {
                GHAppInstallationToken installationToken = createApplicationGitHub().getApp()
                        .getInstallationById(installationId)
                        .createToken().create();

                return new CachedInstallationToken(installationToken.getToken(), installationToken.getExpiresAt());
            } catch (IOException e) {
                throw new IllegalStateException("Unable to create a GitHub client for the installation " + installationId, e);
            }
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
            return new GitHubBuilder().withConnector(okhttpConnector).withJwtToken(jwtToken).build();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create a GitHub client for the application", e);
        }
    }
}
