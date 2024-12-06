package io.quarkiverse.githubapp;

import org.kohsuke.github.GitHubBuilder;

/**
 * Provide a CDI bean implementing this interface to customize {@link GitHubBuilder} used to create the
 * {@link org.kohsuke.github.GitHub} instances.
 */
public interface GitHubCustomizer {

    /**
     * Customize the {@link GitHubBuilder} with various options.
     *
     * <p>
     * These customizations are applied to both application and installation clients. Known cases of customizations that
     * may be used with installation clients but not with application clients include configuring a rate limit checker. To use a
     * different set of customizations for the application client, implement {@link #customizeApplicationClient(GitHubBuilder)}.
     * <p>
     * Note that customizations that use {@link GitHubBuilder#withAppInstallationToken(String)} and
     * {@link GitHubBuilder#withEndpoint(String)} are eventually overridden by the
     * {@link io.quarkiverse.githubapp.runtime.github.GitHubService}. Installation tokens are created and cached by the service.
     * <p>
     * To specify a custom endpoint, use configuration properties {@code quarkus.github-app.instance-endpoint} or
     * {@code quarkus.github-app.rest-api-endpoint}.
     *
     * @param builder to customize
     */
    void customize(GitHubBuilder builder);

    /**
     * Customize the {@link GitHubBuilder} with various options specifically for the application client. Default implementation
     * delegates to {@link #customize(GitHubBuilder)} for backwards compatibility.
     * <p>
     * Note that customizations should never use {@link GitHubBuilder#withAppInstallationToken(String)}, because this is the
     * application client. Customizations that use {@link GitHubBuilder#withJwtToken(String)} and
     * {@link GitHubBuilder#withEndpoint(String)} are eventually overridden by the service.
     *
     * <p>
     * To specify a custom endpoint, use configuration properties {@code quarkus.github-app.instance-endpoint} or
     * {@code quarkus.github-app.rest-api-endpoint}.
     *
     * @param builder to customize
     */
    default void customizeApplicationClient(GitHubBuilder builder) {
        customize(builder);
    }
}
