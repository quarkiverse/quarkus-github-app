package io.quarkiverse.githubapp.runtime;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.inject.Qualifier;

@Target(TYPE)
@Retention(RUNTIME)
@Qualifier
public @interface Multiplexer {
}
