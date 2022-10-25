package command.airline;

import java.io.IOException;

import org.kohsuke.github.GHEventPayload;

import com.github.rvesse.airline.annotations.Cli;
import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.model.CommandMetadata;
import com.github.rvesse.airline.model.GlobalMetadata;

import command.airline.InjectMetadataCli.Command1;
import io.quarkiverse.githubapp.command.airline.AirlineInject;

// tag::include[]
@Cli(name = "@bot", commands = { Command1.class })
public class InjectMetadataCli {

    interface Commands {

        void run(GHEventPayload.IssueComment issueCommentPayload) throws IOException;
    }

    @Command(name = "command1")
    static class Command1 implements Commands {

        @AirlineInject // <1>
        GlobalMetadata<InjectMetadataCli> globalMetadata;

        @AirlineInject // <1>
        CommandMetadata commandMetadata;

        @Override
        public void run(GHEventPayload.IssueComment issueCommentPayload) throws IOException {
            // ...
        }
    }
}
// end::include[]
