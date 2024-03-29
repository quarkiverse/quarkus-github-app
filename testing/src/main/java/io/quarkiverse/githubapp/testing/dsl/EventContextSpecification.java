package io.quarkiverse.githubapp.testing.dsl;

public interface EventContextSpecification {
    <T extends Throwable> EventContextSpecification github(GitHubMockSetup<T> gitHubMockSetup) throws T;

    EventSenderOptions when();

    <T extends Throwable> EventHandlingResponse when(TestedAction<T> action) throws T;
}
