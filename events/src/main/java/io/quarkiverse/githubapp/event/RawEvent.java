package io.quarkiverse.githubapp.event;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.inject.Qualifier;

/**
 * Allows listening to any event and obtain a raw {@code GitHubEvent}.
 * <p>
 * Especially useful when we don't have a dedicated annotation for a given event.
 * <p>
 * For instance:
 * {@code @RawEvent(event = "pull_request", action = "opened") GitHubEvent gitHubEvent}
 */
@Target({ PARAMETER, TYPE })
@Retention(RUNTIME)
@Qualifier
public @interface RawEvent {

    String event() default Events.ALL;

    String action() default Actions.ALL;
}
