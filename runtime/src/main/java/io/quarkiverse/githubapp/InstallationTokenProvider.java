package io.quarkiverse.githubapp;

import java.time.Instant;

import org.kohsuke.github.GitHub;

import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClient;

/**
 * A provider of installation tokens for the GitHub app.
 * <p>
 * Inject as a CDI bean.
 * <p>
 * <strong>NOTE:</strong> You generally will not need this bean when processing events,
 * as clients can be automatically injected into event listener methods,
 * simply by adding a parameter of type {@link GitHub} or {@link DynamicGraphQLClient} to the listener method.
 * This provider is mostly useful for advanced use cases, e.g. when you need to access the GitHub API directly.
 */
public interface InstallationTokenProvider {

    /**
     * Gets a valid installation token for a given application installation.
     * <p>
     * The token will remain functional a few minutes,
     * so you should discard it after your unit of work and retrieve another one when necessary.
     * <p>
     * <strong>NOTE:</strong> You generally will not need this method when processing events,
     * as clients can be automatically injected into event listener listener methods,
     * simply by adding a parameter of type {@link GitHub} to the method.
     * This method can still be useful for advanced use cases, e.g. when you need to access the GitHub API directly.
     *
     * @return The client for the given installation.
     */
    InstallationToken getInstallationToken(long installationId);

    public interface InstallationToken {

        String token();

        Instant expiresAt();
    }
}
