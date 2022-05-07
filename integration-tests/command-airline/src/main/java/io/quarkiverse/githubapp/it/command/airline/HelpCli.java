package io.quarkiverse.githubapp.it.command.airline;

import java.io.IOException;

import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHEventPayload.IssueComment;

import com.github.rvesse.airline.annotations.Cli;
import com.github.rvesse.airline.annotations.Command;

import io.quarkiverse.githubapp.command.airline.AbstractHelpCommand;
import io.quarkiverse.githubapp.it.command.airline.HelpCli.HelpCommand;
import io.quarkiverse.githubapp.it.command.airline.HelpCli.TestCommand1;

@Cli(name = "@help", description = "Testing help generation", commands = { TestCommand1.class, HelpCommand.class })
public class HelpCli {

    @Command(name = "command1", description = "Command 1")
    static class TestCommand1 implements TestCommand {

        @Override
        public void run(IssueComment issueCommentPayload) throws IOException {
            // do nothing
        }
    }

    @Command(name = "help", description = "Push a message with help")
    static class HelpCommand extends AbstractHelpCommand implements TestCommand {
    }

    public interface TestCommand {

        void run(GHEventPayload.IssueComment issueCommentPayload) throws IOException;
    }
}
