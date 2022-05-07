package command.airline;

import java.io.IOException;

import org.kohsuke.github.GHEventPayload;

import com.github.rvesse.airline.annotations.Cli;
import com.github.rvesse.airline.annotations.Command;

import command.airline.CommandOptionsCli.CommandOnlyForIssues;
import io.quarkiverse.githubapp.command.airline.CommandOptions;
import io.quarkiverse.githubapp.command.airline.CommandOptions.CommandScope;
import io.quarkiverse.githubapp.command.airline.CommandOptions.ExecutionErrorStrategy;

// tag::include[]
@Cli(name = "@bot", commands = { CommandOnlyForIssues.class })
public class CommandOptionsCli {

    interface Commands {

        void run(GHEventPayload.IssueComment issueCommentPayload) throws IOException;
    }

    // tag::only-for-issues[]
    @Command(name = "only-for-issues")
    @CommandOptions(scope = CommandScope.ISSUES) // <1>
    static class CommandOnlyForIssues implements Commands {

        @Override
        public void run(GHEventPayload.IssueComment issueCommentPayload) throws IOException {
            // do something
        }
    }
    // end::only-for-issues[]

    // tag::only-for-prs[]
    @Command(name = "only-for-pull-requests")
    @CommandOptions(scope = CommandScope.PULL_REQUESTS) // <1>
    static class CommandOnlyForPullRequests implements Commands {

        @Override
        public void run(GHEventPayload.IssueComment issueCommentPayload) throws IOException {
            // do something
        }
    }
    // end::only-for-prs[]

    // tag::execution-error-strategy[]
    @Command(name = "execution-error-strategy")
    @CommandOptions(executionErrorStrategy = ExecutionErrorStrategy.COMMENT_MESSAGE) // <1>
    static class CommandWithCustomExecutionErrorStrategy implements Commands {

        @Override
        public void run(GHEventPayload.IssueComment issueCommentPayload) throws IOException {
            // do something
        }
    }
    // end::execution-error-strategy[]

    // tag::execution-error-message[]
    @Command(name = "execution-error-message")
    @CommandOptions(executionErrorStrategy = ExecutionErrorStrategy.COMMENT_MESSAGE, executionErrorMessage = "Your custom error message") // <1>
    static class CommandWithCustomExecutionErrorMessage implements Commands {

        @Override
        public void run(GHEventPayload.IssueComment issueCommentPayload) throws IOException {
            // do something
        }
    }
    // end::execution-error-message[]
}
// end::include[]
