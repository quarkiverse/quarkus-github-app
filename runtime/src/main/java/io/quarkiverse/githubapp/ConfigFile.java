package io.quarkiverse.githubapp;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.inject.Qualifier;

@Target(PARAMETER)
@Retention(RUNTIME)
@Qualifier
public @interface ConfigFile {

    String value();
}