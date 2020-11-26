package io.quarkiverse.githubapp.testing.dsl;

public interface ValidatableEventHandling {
    <T extends Throwable> ValidatableEventHandling github(GitHubMockVerification<T> verification) throws T;
}
