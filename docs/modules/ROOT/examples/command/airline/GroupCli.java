package command.airline;

import java.io.IOException;

import org.kohsuke.github.GHEventPayload;

import com.github.rvesse.airline.annotations.Cli;
import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Group;

import command.airline.GroupCli.ListCommand;
import command.airline.GroupCli.ShowCommand;

// tag::include[]
@Cli(name = "@bot", groups = { @Group(name = "remote", commands = { ListCommand.class, ShowCommand.class }) }) // <1>
public class GroupCli {

    interface Commands {

        void run(GHEventPayload.IssueComment issueCommentPayload) throws IOException;
    }

    @Command(name = "list") // <2>
    static class ListCommand implements Commands {

        @Override
        public void run(GHEventPayload.IssueComment issueCommentPayload) throws IOException {
            // do something
        }
    }

    @Command(name = "show") // <3>
    static class ShowCommand implements Commands {

        @Override
        public void run(GHEventPayload.IssueComment issueCommentPayload) throws IOException {
            // do something
        }
    }
}
// end::include[]
