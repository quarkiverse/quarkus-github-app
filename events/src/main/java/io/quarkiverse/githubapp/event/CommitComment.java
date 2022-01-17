package io.quarkiverse.githubapp.event;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.inject.Qualifier;

import org.kohsuke.github.GHEventPayload;

@Event(name = "commit_comment", payload = GHEventPayload.CommitComment.class)
@Target({ PARAMETER, TYPE })
@Retention(RUNTIME)
@Qualifier
public @interface CommitComment {

    String value() default Actions.ALL;

    @CommitComment(Created.NAME)
    @Target(PARAMETER)
    @Retention(RUNTIME)
    @Qualifier
    public @interface Created {

        String NAME = Actions.CREATED;
    }
}
