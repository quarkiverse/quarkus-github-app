package io.quarkiverse.githubapp.testing;

import io.quarkiverse.githubapp.testing.internal.GitHubAppTestingCallback;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.util.HashMap;
import java.util.Map;

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
