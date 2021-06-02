package io.quarkiverse.githubapp.testing.internal;

import java.util.Map;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

import io.quarkiverse.githubapp.error.ErrorHandler;
import io.quarkiverse.githubapp.runtime.github.GitHubFileDownloader;
import io.quarkiverse.githubapp.runtime.github.GitHubService;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.callback.QuarkusTestAfterConstructCallback;

public final class GitHubAppTestingCallback
        implements QuarkusTestAfterConstructCallback {

    private static final String ENABLED_KEY = "quarkiverse-github-app-testing.enabled";

    public static void enable(Map<String, String> configProperties) {
        configProperties.put(ENABLED_KEY, "true");
    }

    static boolean isEnabled() {
        return ConfigProviderResolver.instance().getConfig().getValue(ENABLED_KEY, Boolean.class);
    }

    @Override
    public void afterConstruct(Object testInstance) {
        if (!isEnabled()) {
            GitHubAppTestingContext.reset();
            return;
        }
        GitHubAppTestingContext.set(testInstance);
        GitHubAppTestingContext context = GitHubAppTestingContext.get();
        GitHubMockContextImpl mocks = context.mocks;
        QuarkusMock.installMockForType(mocks.service, GitHubService.class);
        QuarkusMock.installMockForType(mocks.fileDownloader, GitHubFileDownloader.class);
        QuarkusMock.installMockForType(context.errorHandler, ErrorHandler.class);
    }
}
