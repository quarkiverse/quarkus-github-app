package io.quarkiverse.githubapp.it.command.airline;

import java.io.IOException;

import javax.inject.Inject;

import org.kohsuke.github.GHEventPayload;

import com.github.rvesse.airline.annotations.Cli;
import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;

import io.quarkiverse.githubapp.it.command.airline.InjectCompositionCli.TestInjectCompositionCommand;

@Cli(name = "@inject-composition", commands = { TestInjectCompositionCommand.class })
public class InjectCompositionCli {

    @Command(name = "test")
    static class TestInjectCompositionCommand implements DefaultCommand {

        @Inject
        VerboseModule verboseModule = new VerboseModule();

        @Override
        public void run(GHEventPayload.IssueComment issueCommentPayload) throws IOException {
            if (verboseModule.verbose) {
                issueCommentPayload.getIssue().comment("hello from @inject-composition test - verbose");
            } else {
                issueCommentPayload.getIssue().comment("hello from @inject-composition test");
            }
        }
    }

    public static class VerboseModule {

        @Option(name = { "-v", "--verbose" }, description = "Enables verbose mode")
        protected boolean verbose = false;
    }

    public interface DefaultCommand {

        void run(GHEventPayload.IssueComment issueCommentPayload) throws IOException;
    }
}
