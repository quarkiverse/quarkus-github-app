package command.airline;

import java.io.IOException;

import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GitHub;

import com.github.rvesse.airline.annotations.Cli;
import com.github.rvesse.airline.annotations.Command;

import command.airline.HelpCli.Command1;
import command.airline.HelpCli.Command2;
import command.airline.HelpCli.HelpCommand;
import io.quarkiverse.githubapp.command.airline.AbstractHelpCommand;

// tag::include[]
@Cli(name = "@bot", commands = { Command1.class, Command2.class,
        HelpCommand.class }, description = "Your helpful bot doing all sorts of things") // <1>
public class HelpCli {

    interface Commands {

        void run(GHEventPayload.IssueComment issueCommentPayload, GitHub gitHub) throws IOException;
    }

    @Command(name = "command1", description = "Do command1 with style") // <2>
    static class Command1 implements Commands {

        @Override
        public void run(GHEventPayload.IssueComment issueCommentPayload, GitHub gitHub) throws IOException {
            // do something
        }
    }

    @Command(name = "command2", description = "Do command2 with style")
    static class Command2 implements Commands {

        @Override
        public void run(GHEventPayload.IssueComment issueCommentPayload, GitHub gitHub) throws IOException {
            // do something
        }
    }

    @Command(name = "help", description = "Print help")
    static class HelpCommand extends AbstractHelpCommand implements Commands { // <3>

        @Override
        public void run(GHEventPayload.IssueComment issueCommentPayload, GitHub gitHub) throws IOException {
            super.run(issueCommentPayload); // <4>
        }
    }
}
// end::include[]
