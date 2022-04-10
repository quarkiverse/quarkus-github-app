package io.quarkiverse.githubapp.command.airline;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.github.rvesse.airline.annotations.Command;

/**
 * Complement to the {@link Command} annotation for Quarkus GitHub App specific options.
 */
@Target(TYPE)
@Retention(RUNTIME)
@Documented
public @interface CommandOptions {

    // Make sure to keep these consistent with the annotation defaults
    CommandScope DEFAULT_SCOPE = CommandScope.ISSUES_AND_PULL_REQUESTS;
    ExecutionErrorStrategy DEFAULT_EXECUTION_ERROR_STRATEGY = ExecutionErrorStrategy.NONE;
    String DEFAULT_EXECUTION_ERROR_MESSAGE = ":warning: An error occurred while executing command: %s";

    /**
     * Whether the command should target issues, pull requests or both.
     */
    CommandScope scope() default CommandScope.ISSUES_AND_PULL_REQUESTS;

    /**
     * The error strategy when an error occurs executing the command.
     */
    ExecutionErrorStrategy executionErrorStrategy() default ExecutionErrorStrategy.NONE;

    /**
     * The error message when an error occurs executing the command.
     */
    String executionErrorMessage() default DEFAULT_EXECUTION_ERROR_MESSAGE;

    public enum ExecutionErrorStrategy {

        NONE(false),
        COMMENT_MESSAGE(true);

        private final boolean message;

        ExecutionErrorStrategy(boolean message) {
            this.message = message;
        }

        public boolean addMessage() {
            return message;
        }
    }

    public enum CommandScope {

        ISSUES,
        PULL_REQUESTS,
        ISSUES_AND_PULL_REQUESTS;

        public boolean isInScope(boolean isPullRequest) {
            if (isPullRequest) {
                return this == ISSUES_AND_PULL_REQUESTS || this == PULL_REQUESTS;
            } else {
                return this == ISSUES_AND_PULL_REQUESTS || this == ISSUES;
            }
        }
    }
}
