package io.quarkiverse.githubapp.event;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.inject.Qualifier;

import org.kohsuke.github.GHEventPayload;

@Event(name = "member", payload = GHEventPayload.Member.class)
@Target({ PARAMETER, TYPE })
@Retention(RUNTIME)
@Qualifier
public @interface Member {

    String value() default Actions.ALL;

    @Member(Added.NAME)
    @Target(PARAMETER)
    @Retention(RUNTIME)
    @Qualifier
    public @interface Added {

        String NAME = Actions.ADDED;
    }

    @Member(Edited.NAME)
    @Target(PARAMETER)
    @Retention(RUNTIME)
    @Qualifier
    public @interface Edited {

        String NAME = Actions.EDITED;
    }

    @Member(Removed.NAME)
    @Target(PARAMETER)
    @Retention(RUNTIME)
    @Qualifier
    public @interface Removed {

        String NAME = Actions.REMOVED;
    }
}
