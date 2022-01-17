package io.quarkiverse.githubapp.event;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.inject.Qualifier;

import org.kohsuke.github.GHEventPayload;

@Event(name = "discussion", payload = GHEventPayload.Discussion.class)
@Target({ PARAMETER, TYPE })
@Retention(RUNTIME)
@Qualifier
public @interface Discussion {

    String value() default Actions.ALL;

    @Discussion(Answered.NAME)
    @Target(PARAMETER)
    @Retention(RUNTIME)
    @Qualifier
    public @interface Answered {

        String NAME = Actions.ANSWERED;
    }

    @Discussion(CategoryChanged.NAME)
    @Target(PARAMETER)
    @Retention(RUNTIME)
    @Qualifier
    public @interface CategoryChanged {

        String NAME = Actions.CATEGORY_CHANGED;
    }

    @Discussion(Created.NAME)
    @Target(PARAMETER)
    @Retention(RUNTIME)
    @Qualifier
    public @interface Created {

        String NAME = Actions.CREATED;
    }

    @Discussion(Deleted.NAME)
    @Target(PARAMETER)
    @Retention(RUNTIME)
    @Qualifier
    public @interface Deleted {

        String NAME = Actions.DELETED;
    }

    @Discussion(Edited.NAME)
    @Target(PARAMETER)
    @Retention(RUNTIME)
    @Qualifier
    public @interface Edited {

        String NAME = Actions.EDITED;
    }

    @Discussion(Labeled.NAME)
    @Target(PARAMETER)
    @Retention(RUNTIME)
    @Qualifier
    public @interface Labeled {

        String NAME = Actions.LABELED;
    }

    @Discussion(Locked.NAME)
    @Target(PARAMETER)
    @Retention(RUNTIME)
    @Qualifier
    public @interface Locked {

        String NAME = Actions.LOCKED;
    }

    @Discussion(Pinned.NAME)
    @Target(PARAMETER)
    @Retention(RUNTIME)
    @Qualifier
    public @interface Pinned {

        String NAME = Actions.PINNED;
    }

    @Discussion(Transferred.NAME)
    @Target(PARAMETER)
    @Retention(RUNTIME)
    @Qualifier
    public @interface Transferred {

        String NAME = Actions.TRANSFERRED;
    }

    @Discussion(Unanswered.NAME)
    @Target(PARAMETER)
    @Retention(RUNTIME)
    @Qualifier
    public @interface Unanswered {

        String NAME = Actions.UNANSWERED;
    }

    @Discussion(Unlabeled.NAME)
    @Target(PARAMETER)
    @Retention(RUNTIME)
    @Qualifier
    public @interface Unlabeled {

        String NAME = Actions.UNLABELED;
    }

    @Discussion(Unlocked.NAME)
    @Target(PARAMETER)
    @Retention(RUNTIME)
    @Qualifier
    public @interface Unlocked {

        String NAME = Actions.UNLOCKED;
    }

    @Discussion(Unpinned.NAME)
    @Target(PARAMETER)
    @Retention(RUNTIME)
    @Qualifier
    public @interface Unpinned {

        String NAME = Actions.UNPINNED;
    }

}
