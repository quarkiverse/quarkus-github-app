package io.quarkiverse.githubapp.testing.internal;

import com.google.common.base.Charsets;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;

public final class GitHubAppTestingContext {

    private static GitHubAppTestingContext instance;

    public static void set(Object testInstance) {
        instance = new GitHubAppTestingContext(testInstance, new GitHubMockContextImpl());
    }

    public static GitHubAppTestingContext get() {
        return instance;
    }

    public static void reset() {
        instance = null;
    }

    public final Object testInstance;
    public final GitHubMockContextImpl mocks;

    private GitHubAppTestingContext(Object testInstance, GitHubMockContextImpl mocks) {
        this.testInstance = testInstance;
        this.mocks = mocks;
    }

    String getFromClasspath(String path) throws IOException {
        try (InputStream stream = testInstance.getClass().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalArgumentException("No such file in classpath: '" + path + "'");
            }
            return IOUtils.toString(stream, Charsets.UTF_8);
        }
    }
}
