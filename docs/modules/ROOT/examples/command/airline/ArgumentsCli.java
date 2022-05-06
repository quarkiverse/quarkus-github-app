package command.airline;

import java.io.IOException;
import java.util.List;

import org.kohsuke.github.GHEventPayload;

import com.github.rvesse.airline.annotations.Arguments;
import com.github.rvesse.airline.annotations.Cli;
import com.github.rvesse.airline.annotations.Command;

import command.airline.ArgumentsCli.CommandWithArguments;

// tag::include[]
@Cli(name = "@bot", commands = { CommandWithArguments.class })
public class ArgumentsCli {

    interface Commands {

        void run(GHEventPayload.IssueComment issueCommentPayload) throws IOException;
    }

    @Command(name = "add-users")
    static class CommandWithArguments implements Commands {

        @Arguments(description = "List of GitHub usernames") // <1>
        List<String> usernames;

        @Override
        public void run(GHEventPayload.IssueComment issueCommentPayload) throws IOException {
            issueCommentPayload.getIssue().comment("Hello " + String.join(", ", usernames)); // <2>
        }
    }
}
// end::include[]
