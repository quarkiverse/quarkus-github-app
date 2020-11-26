package io.quarkiverse.githubapp.testing.dsl;

@FunctionalInterface
public interface GitHubMockVerification<T extends Throwable> {

    void verify(GitHubMockVerificationContext mocks) throws T;

}
