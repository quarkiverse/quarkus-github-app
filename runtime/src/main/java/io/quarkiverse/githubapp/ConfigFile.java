package io.quarkiverse.githubapp;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target(PARAMETER)
@Retention(RUNTIME)
public @interface ConfigFile {

    String value();

    Source source() default Source.DEFAULT;

    public enum Source {
        DEFAULT,
        SOURCE_REPOSITORY,
        CURRENT_REPOSITORY;
    }
}