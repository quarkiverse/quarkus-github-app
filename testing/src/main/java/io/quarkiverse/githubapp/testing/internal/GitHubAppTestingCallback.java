package io.quarkiverse.githubapp.testing.internal;

import io.quarkiverse.githubapp.runtime.github.GitHubFileDownloader;
import io.quarkiverse.githubapp.runtime.github.GitHubService;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.callback.QuarkusTestAfterConstructCallback;
import io.quarkus.test.junit.callback.QuarkusTestAfterEachCallback;
import io.quarkus.test.junit.callback.QuarkusTestBeforeEachCallback;
import io.quarkus.test.junit.callback.QuarkusTestMethodContext;

import java.util.Map;

public final class GitHubAppTestingCallback
        implements QuarkusTestAfterConstructCallback,
        QuarkusTestBeforeEachCallback, QuarkusTestAfterEachCallback {

    private static final String ENABLED_KEY = "quarkiverse-github-app-testing.enabled";

    public static void enable(Map<String, String> systemProperties) {
        systemProperties.put(ENABLED_KEY, "true");
    }

    static boolean isEnabled() {
        return Boolean.getBoolean(ENABLED_KEY);
    }

    @Override
    public void afterConstruct(Object testInstance) {
        if (!isEnabled()) {
            GitHubAppTestingContext.reset();
            return;
        }
        GitHubAppTestingContext.set(testInstance);
        GitHubMockContextImpl mocks = GitHubAppTestingContext.get().mocks;
        QuarkusMock.installMockForType(mocks.service, GitHubService.class);
        QuarkusMock.installMockForType(mocks.fileDownloader, GitHubFileDownloader.class);
    }

    @Override
    public void beforeEach(QuarkusTestMethodContext context) {
        if (!isEnabled()) {
            return;
        }
        GitHubAppTestingContext.get().mocks.init();
    }

    @Override
    public void afterEach(QuarkusTestMethodContext context) {
        if (!isEnabled()) {
            return;
        }
        GitHubAppTestingContext.get().mocks.reset();
    }
}
