package io.quarkiverse.githubapp.testing;

import java.util.HashMap;
import java.util.Map;

import io.quarkiverse.githubapp.testing.internal.GitHubAppTestingCallback;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public final class GitHubAppTestingResource implements QuarkusTestResourceLifecycleManager {

    @Override
    public Map<String, String> start() {
        Map<String, String> systemProperties = new HashMap<>();
        GitHubAppTestingCallback.enable(systemProperties);
        return systemProperties;
    }

    @Override
    public void stop() {
        // Nothing to do
    }

}
