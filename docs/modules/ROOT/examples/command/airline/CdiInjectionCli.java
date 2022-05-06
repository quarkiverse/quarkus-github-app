package command.airline;

import java.io.IOException;

import javax.inject.Inject;

import org.kohsuke.github.GHEventPayload;

import com.github.rvesse.airline.annotations.Cli;
import com.github.rvesse.airline.annotations.Command;

import command.airline.CdiInjectionCli.Command1;

// tag::include[]
@Cli(name = "@bot", commands = { Command1.class })
public class CdiInjectionCli {

    interface Commands {

        void run(GHEventPayload.IssueComment issueCommentPayload) throws IOException;
    }

    @Command(name = "command1")
    static class Command1 implements Commands {

        @Inject
        CdiBean cdiBean; // <1>

        @Override
        public void run(GHEventPayload.IssueComment issueCommentPayload) throws IOException {
            cdiBean.doSomething();
        }
    }
}
// end::include[]
