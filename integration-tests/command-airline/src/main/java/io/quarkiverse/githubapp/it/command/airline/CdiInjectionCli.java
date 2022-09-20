package io.quarkiverse.githubapp.it.command.airline;

import java.io.IOException;
import java.time.Clock;
import java.time.ZoneOffset;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.kohsuke.github.GHEventPayload;

import com.github.rvesse.airline.annotations.Cli;
import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.model.GlobalMetadata;

import io.quarkiverse.githubapp.it.command.airline.CdiInjectionCli.TestCommand;

@Cli(name = "@cdi-injection", commands = { TestCommand.class })
public class CdiInjectionCli {

    @Command(name = "test")
    static class TestCommand implements CdiInjectionCommand {

        @Inject
        GlobalMetadata<CdiInjectionCommand> globalMetadata;

        @Inject
        Service1 service1;

        @Inject
        Clock clock;

        @Override
        public void run(GHEventPayload.IssueComment issueCommentPayload, Service2 service2) throws IOException {
            issueCommentPayload.getIssue()
                    .comment(service1.hello() + " - " + service2.hello() + " - " + globalMetadata.getName());
        }
    }

    public interface CdiInjectionCommand {

        void run(GHEventPayload.IssueComment issueCommentPayload, Service2 service2) throws IOException;
    }

    @Singleton
    public static class Service1 {

        public static final String HELLO = "hello from service1";

        public String hello() {
            return HELLO;
        }
    }

    @Singleton
    public static class Service2 {

        public static final String HELLO = "hello from service2";

        public String hello() {
            return HELLO;
        }
    }

    @Singleton
    public static class ClockProducer {
        @Produces
        @ApplicationScoped
        public Clock clock() {
            return Clock.system(ZoneOffset.UTC);
        }
    }
}
