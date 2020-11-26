package io.quarkiverse.githubapp.testing.internal;

import io.quarkiverse.githubapp.testing.dsl.EventHandlingResponse;

final class EventHandlingResponseImpl implements EventHandlingResponse {

    private final GitHubAppTestingContext testingContext;

    EventHandlingResponseImpl(GitHubAppTestingContext testingContext) {
        this.testingContext = testingContext;
    }

    @Override
    public ValidatableEventHandlingImpl then() {
        return new ValidatableEventHandlingImpl(testingContext);
    }
}
