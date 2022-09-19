package io.quarkiverse.githubapp.command.airline;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.github.rvesse.airline.annotations.Cli;

/**
 * Complement to the {@link Cli} annotation for Quarkus GitHub App specific options.
 */
@Target(TYPE)
@Retention(RUNTIME)
@Documented
public @interface CliOptions {

    // Make sure to keep these consistent with the annotation defaults
    ParseErrorStrategy DEFAULT_PARSE_ERROR_STRATEGY = ParseErrorStrategy.COMMENT_MESSAGE_HELP_ERRORS;
    String DEFAULT_PARSE_ERROR_MESSAGE = "> `%s`\n\n:rotating_light: Unable to parse the command.";

    /**
     * The aliases of the command. They will be recognized as triggering this particular command set.
     */
    String[] aliases() default {};

    /**
     * The error strategy when an error occurs parsing the command.
     */
    ParseErrorStrategy parseErrorStrategy() default ParseErrorStrategy.COMMENT_MESSAGE_HELP_ERRORS;

    /**
     * The error message when an error occurs parsing the command.
     */
    String parseErrorMessage() default DEFAULT_PARSE_ERROR_MESSAGE;

    /**
     * Default options applied to all this command set, except if they are overridden at the command level.
     */
    CommandOptions defaultCommandOptions() default @CommandOptions;

    public enum ParseErrorStrategy {

        NONE(false, false, false),
        COMMENT_MESSAGE(true, false, false),
        COMMENT_MESSAGE_HELP(true, true, false),
        COMMENT_MESSAGE_ERRORS(true, false, true),
        COMMENT_MESSAGE_HELP_ERRORS(true, true, true);

        private final boolean message;
        private final boolean help;
        private final boolean errors;

        ParseErrorStrategy(boolean message, boolean help, boolean errors) {
            this.message = message;
            this.help = help;
            this.errors = errors;
        }

        public boolean addMessage() {
            return message;
        }

        public boolean includeHelp() {
            return help;
        }

        public boolean includeErrors() {
            return errors;
        }
    }
}
