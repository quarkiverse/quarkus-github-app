package io.quarkiverse.githubapp.testing.internal;

import io.quarkiverse.githubapp.testing.dsl.GitHubMockVerification;
import io.quarkiverse.githubapp.testing.dsl.ValidatableEventHandling;

final class ValidatableEventHandlingImpl implements ValidatableEventHandling {

    private final GitHubAppTestingContext testingContext;

    ValidatableEventHandlingImpl(GitHubAppTestingContext testingContext) {
        this.testingContext = testingContext;
    }

    @Override
    public <T extends Throwable> ValidatableEventHandling github(GitHubMockVerification<T> verification) throws T {
        verification.verify(testingContext.mocks);
        return this;
    }

}
