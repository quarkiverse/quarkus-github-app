package io.quarkiverse.githubapp.it.command.airline;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.io.IOException;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;

import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHEventPayload.IssueComment;

import com.github.rvesse.airline.annotations.Cli;
import com.github.rvesse.airline.annotations.Command;

import io.quarkiverse.githubapp.command.airline.CliOptions;
import io.quarkiverse.githubapp.command.airline.CommandOptions;
import io.quarkiverse.githubapp.command.airline.CommandOptions.CommandScope;
import io.quarkiverse.githubapp.it.command.airline.ArcSubclassesCli.TestApplicationScopedCommand1;
import io.quarkiverse.githubapp.it.command.airline.ArcSubclassesCli.TestApplicationScopedCommand2;
import io.quarkiverse.githubapp.it.command.airline.ArcSubclassesCli.TestSubclassCommand1;
import io.quarkiverse.githubapp.it.command.airline.ArcSubclassesCli.TestSubclassCommand2;
import io.quarkiverse.githubapp.it.command.airline.DefaultCommandOptionsCli.TestCommand;

@Cli(name = "@arc", commands = { TestApplicationScopedCommand1.class, TestApplicationScopedCommand2.class,
        TestSubclassCommand1.class, TestSubclassCommand2.class })
@CliOptions(defaultCommandOptions = @CommandOptions(scope = CommandScope.ISSUES))
public class ArcSubclassesCli {

    @Command(name = "application-scoped-command1")
    @ApplicationScoped
    static class TestApplicationScopedCommand1 implements TestCommand {

        @Override
        public void run(IssueComment issueCommentPayload) throws IOException {
            issueCommentPayload.getIssue().comment("hello from @arc application-scoped-command1");
        }
    }

    @Command(name = "application-scoped-command2")
    @CommandOptions(scope = CommandScope.ISSUES_AND_PULL_REQUESTS)
    @ApplicationScoped
    static class TestApplicationScopedCommand2 implements TestCommand {

        @Override
        public void run(IssueComment issueCommentPayload) throws IOException {
            issueCommentPayload.getIssue().comment("hello from @arc application-scoped-command2");
        }
    }

    // we add an interceptor to generate a `_Subclass`
    @Command(name = "subclass-command1")
    @DoNothing
    static class TestSubclassCommand1 implements TestCommand {

        @Override
        public void run(IssueComment issueCommentPayload) throws IOException {
            issueCommentPayload.getIssue().comment("hello from @arc subclass-command1");
        }
    }

    // we add an interceptor to generate a `_Subclass`
    @Command(name = "subclass-command2")
    @CommandOptions(scope = CommandScope.ISSUES_AND_PULL_REQUESTS)
    @DoNothing
    static class TestSubclassCommand2 implements TestCommand {

        @Override
        public void run(IssueComment issueCommentPayload) throws IOException {
            issueCommentPayload.getIssue().comment("hello from @arc subclass-command2");
        }
    }

    public interface DefaultCommand {

        void run(GHEventPayload.IssueComment issueCommentPayload) throws IOException;
    }

    @Target({ TYPE, METHOD })
    @Retention(RUNTIME)
    @Documented
    @InterceptorBinding
    public @interface DoNothing {
    }

    @DoNothing
    @Priority(2020)
    @Interceptor
    public static class LoggingInterceptor {

        @AroundInvoke
        Object logInvocation(InvocationContext context) {
            // do nothing specific, we just want an interceptor

            try {
                return context.proceed();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
