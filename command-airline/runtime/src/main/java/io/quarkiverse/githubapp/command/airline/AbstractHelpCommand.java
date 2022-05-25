package io.quarkiverse.githubapp.command.airline;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.kohsuke.github.GHEventPayload;

import com.github.rvesse.airline.annotations.Arguments;
import com.github.rvesse.airline.help.Help;
import com.github.rvesse.airline.model.GlobalMetadata;

public class AbstractHelpCommand {

    @Inject
    public GlobalMetadata<?> global;

    @Arguments
    public List<String> command = new ArrayList<>();

    public void run(GHEventPayload.IssueComment issueCommentPayload) throws IOException {
        try {
            ByteArrayOutputStream helpOs = new ByteArrayOutputStream();
            Help.help(global, command, helpOs);

            issueCommentPayload.getIssue().comment("```\n" + helpOs.toString(StandardCharsets.UTF_8).trim() + "\n```");
        } catch (IOException e) {
            throw new RuntimeException("Error generating usage documentation for " + String.join(" ", command), e);
        }
    }
}
