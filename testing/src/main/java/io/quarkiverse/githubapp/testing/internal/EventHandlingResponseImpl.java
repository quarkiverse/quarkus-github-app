package io.quarkiverse.githubapp.testing.internal;

import io.quarkiverse.githubapp.testing.dsl.EventHandlingResponse;

final class EventHandlingResponseImpl implements EventHandlingResponse {

    private final GitHubAppTestingContext testingContext;
    private final int statusCode;

    EventHandlingResponseImpl(GitHubAppTestingContext testingContext, int statusCode) {
        this.testingContext = testingContext;
        this.statusCode = statusCode;
    }

    @Override
    public int statusCode() {
        return statusCode;
    }

    @Override
    public ValidatableEventHandlingImpl then() {
        return new ValidatableEventHandlingImpl(testingContext);
    }
}
