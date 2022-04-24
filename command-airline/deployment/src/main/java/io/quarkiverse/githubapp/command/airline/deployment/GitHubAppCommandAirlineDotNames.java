package io.quarkiverse.githubapp.command.airline.deployment;

import javax.enterprise.context.Dependent;

import org.jboss.jandex.DotName;
import org.kohsuke.github.GHEventPayload;

import com.github.rvesse.airline.annotations.Alias;
import com.github.rvesse.airline.annotations.Arguments;
import com.github.rvesse.airline.annotations.Cli;
import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
import com.github.rvesse.airline.model.CommandGroupMetadata;
import com.github.rvesse.airline.model.CommandMetadata;
import com.github.rvesse.airline.model.GlobalMetadata;

import io.quarkiverse.githubapp.command.airline.CliOptions;
import io.quarkiverse.githubapp.command.airline.CommandOptions;
import io.quarkiverse.githubapp.command.airline.Permission;
import io.quarkiverse.githubapp.command.airline.Team;
import io.quarkiverse.githubapp.event.IssueComment;

class GitHubAppCommandAirlineDotNames {

    static final DotName DEPENDENT = DotName.createSimple(Dependent.class.getName());

    static final DotName CLI = DotName.createSimple(Cli.class.getName());
    static final DotName ALIAS = DotName.createSimple(Alias.class.getName());
    static final DotName CLI_OPTIONS = DotName.createSimple(CliOptions.class.getName());
    static final DotName COMMAND_OPTIONS = DotName.createSimple(CommandOptions.class.getName());
    static final DotName PERMISSION = DotName.createSimple(Permission.class.getName());
    static final DotName TEAM = DotName.createSimple(Team.class.getName());
    static final DotName COMMAND = DotName.createSimple(Command.class.getName());
    static final DotName OPTION = DotName.createSimple(Option.class.getName());
    static final DotName ARGUMENTS = DotName.createSimple(Arguments.class.getName());

    static final DotName GLOBAL_METADATA = DotName.createSimple(GlobalMetadata.class.getName());
    static final DotName COMMAND_GROUP_METADATA = DotName.createSimple(CommandGroupMetadata.class.getName());
    static final DotName COMMAND_METADATA = DotName.createSimple(CommandMetadata.class.getName());

    static final DotName GH_EVENT_PAYLOAD_ISSUE_COMMENT = DotName.createSimple(GHEventPayload.IssueComment.class.getName());
    static final DotName ISSUE_COMMENT_CREATED = DotName.createSimple(IssueComment.Created.class.getName());
}
