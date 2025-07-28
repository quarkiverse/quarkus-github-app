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
    String DEFAULT_EXECUTION_ERROR_MESSAGE = "> `%s`\n\n:rotating_light: An error occurred while executing the command.";
    ReactionStrategy DEFAULT_REACTION_STRATEGY = ReactionStrategy.ALL;

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

    /**
     * The reaction strategy used to provide feedback via comment reactions.
     */
    ReactionStrategy reactionStrategy() default ReactionStrategy.ALL;

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

    /**
     * Things are a bit specific here:
     * - the command specific option will be honored if possible
     * - if the error happens at a more global level (typically if not able to parse the command), the default command option
     * will be used
     */
    public enum ReactionStrategy {

        NONE(false, false, false),
        ON_PROGRESS(true, false, false),
        ON_PROGRESS_ON_ERROR(true, false, true),
        ON_NORMAL_ON_ERROR(false, true, true),
        ON_ERROR(false, false, true),
        ALL(true, true, true);

        private final boolean reactionOnProgress;
        private final boolean reactionOnNormalFlow;
        private final boolean reactionOnError;

        ReactionStrategy(boolean reactionOnProgress, boolean reactionOnNormalFlow, boolean reactionOnError) {
            this.reactionOnProgress = reactionOnProgress;
            this.reactionOnNormalFlow = reactionOnNormalFlow;
            this.reactionOnError = reactionOnError;
        }

        public boolean reactionOnProgress() {
            return reactionOnProgress;
        }

        public boolean reactionOnNormalFlow() {
            return reactionOnNormalFlow;
        }

        public boolean reactionOnError() {
            return reactionOnError;
        }
    }
}
