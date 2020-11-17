package io.quarkiverse.githubapp.event;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.kohsuke.github.GHEventPayload;

@Event(name = "pull_request", payload = GHEventPayload.PullRequest.class)
@Target({ PARAMETER, TYPE })
@Retention(RUNTIME)
public @interface PullRequest {

    String value() default Actions.ALL;

    @PullRequest(Opened.NAME)
    @Target(PARAMETER)
    @Retention(RUNTIME)
    public @interface Opened {

        String NAME = Actions.OPENED;
    }

    @PullRequest(Edited.NAME)
    @Target(PARAMETER)
    @Retention(RUNTIME)
    public @interface Edited {

        String NAME = Actions.EDITED;
    }
}
