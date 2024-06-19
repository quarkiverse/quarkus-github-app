package io.quarkiverse.githubapp;

import org.kohsuke.github.GitHubBuilder;

import io.quarkiverse.githubapp.runtime.config.CheckedConfigProvider;

/**
 * Seam for customizing the {@link GitHubBuilder} used to create the {@link org.kohsuke.github.GitHub} instance.
 */
public interface GitHubCustomizer {

    /**
     * Customize the {@link GitHubBuilder} with various options for runtime.
     * <p>
     * Note that customizations that use {@link GitHubBuilder#withAppInstallationToken(String)} and
     * {@link GitHubBuilder#withEndpoint(String)} are eventually overridden by the
     * {@link io.quarkiverse.githubapp.runtime.github.GitHubService}. Installation tokens are created and cached by the service.
     * To specify a custom endpoint, prefer the {@link CheckedConfigProvider#restApiEndpoint()}
     *
     * @param builder to customize
     */
    void customize(GitHubBuilder builder);
}
