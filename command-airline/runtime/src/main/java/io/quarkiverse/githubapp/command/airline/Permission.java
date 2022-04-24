package io.quarkiverse.githubapp.command.airline;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.kohsuke.github.GHPermissionType;

import com.github.rvesse.airline.annotations.Cli;
import com.github.rvesse.airline.annotations.Command;

/**
 * Permission required to be authorized to execute the command(s).
 * <p>
 * Can be added on either a class annotated with {@link Cli} or a class annotated with {@link Command}.
 */
@Target(TYPE)
@Retention(RUNTIME)
@Documented
public @interface Permission {

    GHPermissionType value();
}
