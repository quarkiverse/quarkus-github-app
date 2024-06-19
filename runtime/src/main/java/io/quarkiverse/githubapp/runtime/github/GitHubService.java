package io.quarkiverse.githubapp.runtime.github;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.security.GeneralSecurityException;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.kohsuke.github.GHAppInstallationToken;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.connector.GitHubConnector;
import org.kohsuke.github.extras.HttpClientGitHubConnector;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.LoadingCache;

import io.quarkiverse.githubapp.GitHubClientProvider;
import io.quarkiverse.githubapp.GitHubCustomizer;
import io.quarkiverse.githubapp.runtime.config.CheckedConfigProvider;
import io.quarkiverse.githubapp.runtime.signing.JwtTokenCreator;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClient;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClientBuilder;

@ApplicationScoped
public class GitHubService implements GitHubClientProvider {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String AUTHORIZATION_HEADER_BEARER = "Bearer %s";

    private final CheckedConfigProvider checkedConfigProvider;

    private final LoadingCache<Long, CachedInstallationToken> installationTokenCache;

    private final JwtTokenCreator jwtTokenCreator;
    private final GitHubConnector gitHubConnector;
    private final GitHubCustomizer githubCustomizer;

    @Inject
    public GitHubService(
            CheckedConfigProvider checkedConfigProvider,
            JwtTokenCreator jwtTokenCreator,
            Instance<GitHubCustomizer> gitHubCustomizer) {
        this.checkedConfigProvider = checkedConfigProvider;
        this.jwtTokenCreator = jwtTokenCreator;
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
        this.gitHubConnector = new HttpClientGitHubConnector(
                HttpClient.newBuilder().version(Version.HTTP_1_1).followRedirects(HttpClient.Redirect.NEVER).build());
        // if the customizer is not resolvable, we use a no-op customizer
        githubCustomizer = gitHubCustomizer.isResolvable() ? gitHubCustomizer.get() : builder -> {
        };
    }

    @Override
    public GitHub getInstallationClient(long installationId) {
        try {
            return createInstallationClient(installationId);
        } catch (IOException e1) {
            synchronized (this) {
                try {
                    // retry in a synchronized in case the token is invalidated in another thread
                    return createInstallationClient(installationId);
                } catch (IOException e2) {
                    try {
                        // this time we invalidate the token entirely and go for a new token
                        installationTokenCache.invalidate(installationId);
                        return createInstallationClient(installationId);
                    } catch (IOException e3) {
                        throw new IllegalStateException(
                                "Unable to create a GitHub client for the installation " + installationId, e3);
                    }
                }
            }
        }
    }

    @Override
    public DynamicGraphQLClient getInstallationGraphQLClient(long installationId) {
        try {
            return createInstallationGraphQLClient(installationId);
        } catch (IOException | ExecutionException | InterruptedException e1) {
            synchronized (this) {
                try {
                    // retry in a synchronized in case the token is invalidated in another thread
                    return createInstallationGraphQLClient(installationId);
                } catch (IOException | ExecutionException | InterruptedException e2) {
                    try {
                        // this time we invalidate the token entirely and go for a new token
                        installationTokenCache.invalidate(installationId);
                        return createInstallationGraphQLClient(installationId);
                    } catch (IOException | ExecutionException | InterruptedException e3) {
                        throw new IllegalStateException(
                                "Unable to create a GitHub GraphQL client for the installation " + installationId, e3);
                    }
                }
            }
        }
    }

    private GitHub createInstallationClient(long installationId) throws IOException {
        CachedInstallationToken installationToken = installationTokenCache.get(installationId);

        final GitHubBuilder gitHubBuilder = new GitHubBuilder()
                .withConnector(gitHubConnector);
        // apply customizations
        githubCustomizer.customize(gitHubBuilder);
        // configure mandatory defaults
        gitHubBuilder
                .withAppInstallationToken(installationToken.getToken())
                .withEndpoint(checkedConfigProvider.restApiEndpoint());

        GitHub gitHub = gitHubBuilder.build();

        // this call is not counted in the rate limit
        gitHub.getRateLimit();

        return gitHub;
    }

    private DynamicGraphQLClient createInstallationGraphQLClient(long installationId)
            throws IOException, ExecutionException, InterruptedException {
        CachedInstallationToken installationToken = installationTokenCache.get(installationId);

        DynamicGraphQLClient graphQLClient = DynamicGraphQLClientBuilder.newBuilder()
                .url(checkedConfigProvider.graphqlApiEndpoint())
                .header(AUTHORIZATION_HEADER, String.format(AUTHORIZATION_HEADER_BEARER, installationToken.getToken()))
                .build();

        // this call is probably - it's not documented - not counted in the rate limit
        graphQLClient.executeSync("query {\n" +
                "rateLimit {\n" +
                "    limit\n" +
                "    cost\n" +
                "    remaining\n" +
                "    resetAt\n" +
                "  }\n" +
                "}");

        return graphQLClient;
    }

    // Using a lambda leads to a warning
    private class CreateInstallationToken implements CacheLoader<Long, CachedInstallationToken> {

        @Override
        public CachedInstallationToken load(Long installationId) throws Exception {
            try {
                GHAppInstallationToken installationToken = createApplicationGitHub().getApp()
                        .getInstallationById(installationId)
                        .createToken().create();

                return new CachedInstallationToken(installationToken.getToken(), installationToken.getExpiresAt());
            } catch (IOException e) {
                throw new IllegalStateException("Unable to create a GitHub token for the installation " + installationId, e);
            }
        }
    }

    // TODO even if we have a cache for the other one, we should probably also keep this one around for a few minutes
    @Override
    public GitHub getApplicationClient() {
        return createApplicationGitHub();
    }

    private GitHub createApplicationGitHub() {
        String jwtToken;

        try {
            jwtToken = jwtTokenCreator.createJwtToken(checkedConfigProvider.appId(), checkedConfigProvider.privateKey(), 540);
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalStateException("Unable to generate the JWT token", e);
        }

        try {
            final GitHubBuilder gitHubBuilder = new GitHubBuilder()
                    .withConnector(gitHubConnector);
            // apply customizations
            githubCustomizer.customize(gitHubBuilder);
            // configure mandatory defaults
            gitHubBuilder
                    .withJwtToken(jwtToken)
                    .withEndpoint(checkedConfigProvider.restApiEndpoint());

            return gitHubBuilder.build();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create a GitHub client for the application", e);
        }
    }
}
