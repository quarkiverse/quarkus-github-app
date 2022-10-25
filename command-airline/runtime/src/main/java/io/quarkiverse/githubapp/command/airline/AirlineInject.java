package io.quarkiverse.githubapp.command.airline;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.github.rvesse.airline.model.CommandGroupMetadata;
import com.github.rvesse.airline.model.CommandMetadata;
import com.github.rvesse.airline.model.GlobalMetadata;

/**
 * Used to mark a field of a command class as subject of injection by Airline.
 * <p>
 * This is typically used to get Airline to inject {@link GlobalMetadata}, {@link CommandGroupMetadata} or
 * {@link CommandMetadata}.
 */
@Target(FIELD)
@Retention(RUNTIME)
@Documented
public @interface AirlineInject {
}
