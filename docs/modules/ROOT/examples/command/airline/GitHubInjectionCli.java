package command.airline;

import java.io.IOException;

import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GitHub;

import com.github.rvesse.airline.annotations.Cli;
import com.github.rvesse.airline.annotations.Command;

import command.airline.GitHubInjectionCli.Command1;
import command.airline.GitHubInjectionCli.Command2;

// tag::include[]
@Cli(name = "@bot", commands = { Command1.class, Command2.class })
public class GitHubInjectionCli {

    interface Commands {

        void run(GHEventPayload.IssueComment issueCommentPayload, GitHub gitHub) throws IOException; // <1>
    }

    @Command(name = "command1")
    static class Command1 implements Commands {

        @Override
        public void run(GHEventPayload.IssueComment issueCommentPayload, GitHub gitHub) throws IOException {
            // do something with the gitHub client
        }
    }

    @Command(name = "command2")
    static class Command2 implements Commands {

        @Override
        public void run(GHEventPayload.IssueComment issueCommentPayload, GitHub gitHub) throws IOException {
            // do something with the gitHub client
        }
    }
}
// end::include[]
