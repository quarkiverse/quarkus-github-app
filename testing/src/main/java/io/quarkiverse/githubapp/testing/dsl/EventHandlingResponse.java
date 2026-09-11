package io.quarkiverse.githubapp.testing.dsl;

public interface EventHandlingResponse {
    int statusCode();

    ValidatableEventHandling then();
}
