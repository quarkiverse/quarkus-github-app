package io.quarkiverse.githubapp.event;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.kohsuke.github.GHEventPayload;

@Target(TYPE)
@Retention(RUNTIME)
public @interface Event {

    String name();

    Class<? extends GHEventPayload> payload();
}
