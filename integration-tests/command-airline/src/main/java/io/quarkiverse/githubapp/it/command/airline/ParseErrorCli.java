package io.quarkiverse.githubapp.it.command.airline;

import java.io.IOException;

import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHEventPayload.IssueComment;

import com.github.rvesse.airline.annotations.Cli;
import com.github.rvesse.airline.annotations.Command;

import io.quarkiverse.githubapp.it.command.airline.ParseErrorCli.TestCommand1;

@Cli(name = "@parse-error", description = "Default parse error behavior", commands = { TestCommand1.class })
class ParseErrorCli {

    @Command(name = "command1", description = "Command 1")
    static class TestCommand1 implements TestCommand {

        @Override
        public void run(IssueComment issueCommentPayload) throws IOException {
            // do nothing
        }
    }

    public interface TestCommand {

        void run(GHEventPayload.IssueComment issueCommentPayload) throws IOException;
    }
}
