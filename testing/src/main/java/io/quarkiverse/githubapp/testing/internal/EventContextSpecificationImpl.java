package io.quarkiverse.githubapp.testing.internal;

import io.quarkiverse.githubapp.testing.dsl.EventContextSpecification;
import io.quarkiverse.githubapp.testing.dsl.GitHubMockSetup;
import io.quarkiverse.githubapp.testing.dsl.TestedAction;

public final class EventContextSpecificationImpl implements EventContextSpecification {
    private final GitHubAppTestingContext testingContext;

    public EventContextSpecificationImpl(GitHubAppTestingContext testingContext) {
        this.testingContext = testingContext;
        // Support multiple assertions in the same test method
        testingContext.mocks.init();
    }

    @Override
    public <T extends Throwable> EventContextSpecificationImpl github(GitHubMockSetup<T> gitHubMockSetup) throws T {
        gitHubMockSetup.setup(testingContext.mocks);
        return this;
    }

    @Override
    public EventSenderOptionsImpl when() {
        return new EventSenderOptionsImpl(testingContext);
    }

    @Override
    public <T extends Throwable> EventHandlingResponseImpl when(TestedAction<T> action) throws T {
        action.run();
        return new EventHandlingResponseImpl(testingContext);
    }
}
