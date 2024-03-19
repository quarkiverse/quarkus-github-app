package io.quarkiverse.githubapp.event;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.inject.Qualifier;

import org.kohsuke.github.GHEventPayload;

@Event(name = "team", payload = GHEventPayload.Team.class)
@Target({ PARAMETER, TYPE })
@Retention(RUNTIME)
@Qualifier
public @interface Team {

    String value() default Actions.ALL;

    @Team(AddedToRepository.NAME)
    @Target(PARAMETER)
    @Retention(RUNTIME)
    @Qualifier
    public @interface AddedToRepository {

        String NAME = Actions.ADDED_TO_REPOSITORY;
    }

    @Team(Created.NAME)
    @Target(PARAMETER)
    @Retention(RUNTIME)
    @Qualifier
    public @interface Created {

        String NAME = Actions.CREATED;
    }

    @Team(Deleted.NAME)
    @Target(PARAMETER)
    @Retention(RUNTIME)
    @Qualifier
    public @interface Deleted {

        String NAME = Actions.DELETED;
    }

    @Team(Edited.NAME)
    @Target(PARAMETER)
    @Retention(RUNTIME)
    @Qualifier
    public @interface Edited {

        String NAME = Actions.EDITED;
    }

    @Team(RemovedFromRepository.NAME)
    @Target(PARAMETER)
    @Retention(RUNTIME)
    @Qualifier
    public @interface RemovedFromRepository {

        String NAME = Actions.REMOVED_FROM_REPOSITORY;
    }
}
