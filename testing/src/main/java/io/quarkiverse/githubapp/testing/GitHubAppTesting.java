package io.quarkiverse.githubapp.testing;

import io.quarkiverse.githubapp.testing.dsl.EventContextSpecification;
import io.quarkiverse.githubapp.testing.dsl.EventSenderOptions;
import io.quarkiverse.githubapp.testing.internal.EventContextSpecificationImpl;
import io.quarkiverse.githubapp.testing.internal.GitHubAppTestingContext;

public final class GitHubAppTesting {

    private GitHubAppTesting() {
    }

    public static EventContextSpecification given() {
        return new EventContextSpecificationImpl(GitHubAppTestingContext.get());
    }

    public static EventSenderOptions when() {
        return given().when();
    }

}
