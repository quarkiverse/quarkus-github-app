package io.quarkiverse.githubapp;

import org.kohsuke.github.GHApp;
import org.kohsuke.github.GitHub;

import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClient;

/**
 * A provider of {@link org.kohsuke.github.GitHub GitHub clients} for the GitHub app.
 * <p>
 * Inject as a CDI bean.
 * <p>
 * <strong>NOTE:</strong> You generally will not need this bean when processing events,
 * as clients can be automatically injected into event listener methods,
 * simply by adding a parameter of type {@link GitHub} or {@link DynamicGraphQLClient} to the listener method.
 * This provider is mostly useful for non-event use cases (e.g. cron jobs).
 */
public interface GitHubClientProvider {

    /**
     * Gets the {@link GitHub GitHub client} for the application:
     * it can be used without any installation, but has very little access rights (almost as little as an anonymous client).
     * <p>
     * The client will remain functional a few minutes at best,
     * so you should discard it as soon as possible and retrieve another one when necessary.
     * <p>
     * <strong>NOTE:</strong> You generally will not need this method when processing events, as the more powerful
     * {@link #getInstallationClient(long) installation client} gets automatically injected into event listeners.
     * This method can still be useful for non-event use cases (e.g. cron jobs),
     * to {@link GitHub#getApp() retrieve information about the application},
     * in particular {@link GHApp#listInstallations() list application installations}.
     *
     * @return The application client.
     */
    GitHub getApplicationClient();

    /**
     * Gets the {@link GitHub GitHub client} for a given application installation.
     * <p>
     * The client will remain functional a few minutes at best,
     * so you should discard it as soon as possible and retrieve another one when necessary.
     * <p>
     * <strong>NOTE:</strong> You generally will not need this method when processing events,
     * as this client can be automatically injected into event listener listener methods,
     * simply by adding a parameter of type {@link GitHub} to the method.
     * This method can still be useful for non-event use cases (e.g. cron jobs),
     * to retrieve installation clients after having {@link GHApp#listInstallations() list application installations}
     * from the {@link #getApplicationClient() application client}.
     *
     * @return The client for the given installation.
     */
    GitHub getInstallationClient(long installationId);

    /**
     * Gets the {@link DynamicGraphQLClient GraphQL GitHub client} for a given application installation.
     * <p>
     * The client will remain functional a few minutes at best,
     * so you should discard it as soon as possible and retrieve another one when necessary.
     * <p>
     * <strong>NOTE:</strong> You generally will not need this method when processing events,
     * as this client can be automatically injected into event listener methods,
     * simply by adding a parameter of type {@link DynamicGraphQLClient} to the listener method.
     * This method can still be useful for non-event use cases (e.g. cron jobs),
     * to retrieve installation clients after having {@link GHApp#listInstallations() list application installations}
     * from the {@link #getApplicationClient() application client}.
     *
     * @return The client for the given installation.
     */
    DynamicGraphQLClient getInstallationGraphQLClient(long installationId);

}
