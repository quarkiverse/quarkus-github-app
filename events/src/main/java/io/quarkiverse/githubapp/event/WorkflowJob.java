package io.quarkiverse.githubapp.event;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.inject.Qualifier;

import org.kohsuke.github.GHEventPayload;

@Event(name = "workflow_job", payload = GHEventPayload.WorkflowJob.class)
@Target({ PARAMETER, TYPE })
@Retention(RUNTIME)
@Qualifier
public @interface WorkflowJob {

    String value() default Actions.ALL;

    @WorkflowJob(Completed.NAME)
    @Target(PARAMETER)
    @Retention(RUNTIME)
    @Qualifier
    public @interface Completed {

        String NAME = Actions.COMPLETED;
    }

    @WorkflowJob(Queued.NAME)
    @Target(PARAMETER)
    @Retention(RUNTIME)
    @Qualifier
    public @interface Queued {

        String NAME = Actions.QUEUED;
    }

    @WorkflowJob(Requested.NAME)
    @Target(PARAMETER)
    @Retention(RUNTIME)
    @Qualifier
    public @interface Requested {

        String NAME = Actions.REQUESTED;
    }
}
