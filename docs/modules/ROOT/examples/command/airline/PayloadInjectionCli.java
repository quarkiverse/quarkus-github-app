package command.airline;

import java.io.IOException;

import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHPullRequest;

import com.github.rvesse.airline.annotations.Cli;
import com.github.rvesse.airline.annotations.Command;

import command.airline.PayloadInjectionCli.Command1;
import command.airline.PayloadInjectionCli.Command2;

// tag::include[]
@Cli(name = "@bot", commands = { Command1.class, Command2.class })
public class PayloadInjectionCli {

    interface Commands { // <1>

        void run(GHEventPayload.IssueComment issueCommentPayload) throws IOException; // <2>
    }

    @Command(name = "command1")
    static class Command1 implements Commands {

        @Override
        public void run(GHEventPayload.IssueComment issueCommentPayload) throws IOException { // <3>
            issueCommentPayload.getIssue().comment("Ack");
        }
    }

    @Command(name = "command2")
    static class Command2 implements Commands {

        @Override
        public void run(GHEventPayload.IssueComment issueCommentPayload) throws IOException {
            if (issueCommentPayload.getIssue().isPullRequest()) {
                GHPullRequest pullRequest = issueCommentPayload.getRepository()
                        .getPullRequest(issueCommentPayload.getIssue().getNumber()); // <4>

                // do something with the pull request
            }
        }
    }
}
// end::include[]
