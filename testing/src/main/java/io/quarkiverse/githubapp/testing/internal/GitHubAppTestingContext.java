package io.quarkiverse.githubapp.testing.internal;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.mockito.Answers;

import com.google.common.base.Charsets;

import io.quarkiverse.githubapp.testing.GithubAppTest;

public final class GitHubAppTestingContext {

    private static GitHubAppTestingContext instance;

    public static void set(Object testInstance) {
        GithubAppTest annotation = testInstance.getClass().getAnnotation(GithubAppTest.class);
        Answers defaultAnswer = Answers.RETURNS_DEFAULTS;
        if (annotation != null) {
            defaultAnswer = annotation.defaultAnswers();
        }
        instance = new GitHubAppTestingContext(testInstance, new GitHubMockContextImpl(defaultAnswer));
    }

    public static GitHubAppTestingContext get() {
        return instance;
    }

    public static void reset() {
        instance = null;
    }

    public final Object testInstance;
    public final GitHubMockContextImpl mocks;
    public final CapturingErrorHandler errorHandler = new CapturingErrorHandler();

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
