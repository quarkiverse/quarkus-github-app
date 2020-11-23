package io.quarkiverse.githubapp.event;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.inject.Qualifier;

import org.kohsuke.github.GHEventPayload;

@Event(name = "pull_request", payload = GHEventPayload.PullRequest.class)
@Target({ PARAMETER, TYPE })
@Retention(RUNTIME)
@Qualifier
public @interface PullRequest {

    String value() default Actions.ALL;

    @PullRequest(Assigned.NAME)
    @Target(PARAMETER)
    @Retention(RUNTIME)
    @Qualifier
    public @interface Assigned {

        String NAME = Actions.ASSIGNED;
    }

    @PullRequest(Closed.NAME)
    @Target(PARAMETER)
    @Retention(RUNTIME)
    @Qualifier
    public @interface Closed {

        String NAME = Actions.CLOSED;
    }

    @PullRequest(Edited.NAME)
    @Target(PARAMETER)
    @Retention(RUNTIME)
    @Qualifier
    public @interface Edited {

        String NAME = Actions.EDITED;
    }

    @PullRequest(Labeled.NAME)
    @Target(PARAMETER)
    @Retention(RUNTIME)
    @Qualifier
    public @interface Labeled {

        String NAME = Actions.LABELED;
    }

    @PullRequest(Locked.NAME)
    @Target(PARAMETER)
    @Retention(RUNTIME)
    @Qualifier
    public @interface Locked {

        String NAME = Actions.LOCKED;
    }

    @PullRequest(Opened.NAME)
    @Target(PARAMETER)
    @Retention(RUNTIME)
    @Qualifier
    public @interface Opened {

        String NAME = Actions.OPENED;
    }

    @PullRequest(ReadyForReview.NAME)
    @Target(PARAMETER)
    @Retention(RUNTIME)
    @Qualifier
    public @interface ReadyForReview {

        String NAME = Actions.READY_FOR_REVIEW;
    }

    @PullRequest(Reopened.NAME)
    @Target(PARAMETER)
    @Retention(RUNTIME)
    @Qualifier
    public @interface Reopened {

        String NAME = Actions.REOPENED;
    }

    @PullRequest(ReviewRequested.NAME)
    @Target(PARAMETER)
    @Retention(RUNTIME)
    @Qualifier
    public @interface ReviewRequested {

        String NAME = Actions.REVIEW_REQUESTED;
    }

    @PullRequest(ReviewRequestRemoved.NAME)
    @Target(PARAMETER)
    @Retention(RUNTIME)
    @Qualifier
    public @interface ReviewRequestRemoved {

        String NAME = Actions.REVIEW_REQUEST_REMOVED;
    }

    @PullRequest(Synchronize.NAME)
    @Target(PARAMETER)
    @Retention(RUNTIME)
    @Qualifier
    public @interface Synchronize {

        String NAME = Actions.SYNCHRONIZE;
    }

    @PullRequest(Unassigned.NAME)
    @Target(PARAMETER)
    @Retention(RUNTIME)
    @Qualifier
    public @interface Unassigned {

        String NAME = Actions.UNASSIGNED;
    }

    @PullRequest(Unlabeled.NAME)
    @Target(PARAMETER)
    @Retention(RUNTIME)
    @Qualifier
    public @interface Unlabeled {

        String NAME = Actions.UNLABELED;
    }

    @PullRequest(Unlocked.NAME)
    @Target(PARAMETER)
    @Retention(RUNTIME)
    @Qualifier
    public @interface Unlocked {

        String NAME = Actions.UNLOCKED;
    }

}
