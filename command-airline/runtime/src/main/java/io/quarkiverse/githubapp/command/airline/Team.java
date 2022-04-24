package io.quarkiverse.githubapp.command.airline;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.github.rvesse.airline.annotations.Cli;
import com.github.rvesse.airline.annotations.Command;

/**
 * Team required to be authorized to execute the command(s).
 * <p>
 * If several of them are specified, being part of one of them is enough to get the authorization.
 * <p>
 * Can be added on either a class annotated with {@link Cli} or a class annotated with {@link Command}.
 */
@Target(TYPE)
@Retention(RUNTIME)
@Documented
public @interface Team {

    /**
     * The slug of the teams (i.e. what is in the URL of the team page).
     */
    String[] value();
}
