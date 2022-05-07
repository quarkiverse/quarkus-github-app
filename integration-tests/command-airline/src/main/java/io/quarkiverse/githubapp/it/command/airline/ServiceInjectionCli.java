package io.quarkiverse.githubapp.it.command.airline;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GitHub;

import com.github.rvesse.airline.annotations.Cli;
import com.github.rvesse.airline.annotations.Command;

import io.quarkiverse.githubapp.it.command.airline.ServiceInjectionCli.TestCommand;

@Cli(name = "@service-injection", commands = { TestCommand.class })
public class ServiceInjectionCli {

    @Command(name = "test")
    static class TestCommand implements ServiceInjectionCommand {

        @Override
        public void run(GHEventPayload.IssueComment issueCommentPayload, GitHub gitHub)
                throws IOException {
            assertThat(issueCommentPayload).isInstanceOf(GHEventPayload.IssueComment.class);
            assertThat(gitHub).isInstanceOf(GitHub.class);

            issueCommentPayload.getIssue().comment("hello from @service-injection test");
        }
    }

    public interface ServiceInjectionCommand {

        void run(GHEventPayload.IssueComment issueCommentPayload, GitHub gitHub)
                throws IOException;
    }
}
