package io.quarkiverse.githubapp;

import org.kohsuke.github.GitHubBuilder;

/**
 * Provide a CDI bean implementing this interface to customize {@link GitHubBuilder} used to create the
 * {@link org.kohsuke.github.GitHub} instances.
 */
public interface GitHubCustomizer {

    /**
     * Customize the {@link GitHubBuilder} with various options.
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
}
