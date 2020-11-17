package io.quarkiverse.githubapp.event;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.kohsuke.github.GHEventPayload;

@Event(name = "issues", payload = GHEventPayload.Issue.class)
@Target({ PARAMETER, TYPE })
@Retention(RUNTIME)
public @interface Issue {

    String value() default Actions.ALL;

    @Issue(Opened.NAME)
    @Target(PARAMETER)
    @Retention(RUNTIME)
    public @interface Opened {

        String NAME = Actions.OPENED;
    }

    @Issue(Edited.NAME)
    public @interface Edited {

        String NAME = Actions.EDITED;
    }
}
