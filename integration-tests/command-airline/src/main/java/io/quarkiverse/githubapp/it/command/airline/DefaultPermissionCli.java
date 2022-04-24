package io.quarkiverse.githubapp.it.command.airline;

import java.io.IOException;

import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHEventPayload.IssueComment;
import org.kohsuke.github.GHPermissionType;

import com.github.rvesse.airline.annotations.Cli;
import com.github.rvesse.airline.annotations.Command;

import io.quarkiverse.githubapp.command.airline.Permission;
import io.quarkiverse.githubapp.it.command.airline.DefaultPermissionCli.TestAdminPermissionCommand;
import io.quarkiverse.githubapp.it.command.airline.DefaultPermissionCli.TestNoPermissionCommand;
import io.quarkiverse.githubapp.it.command.airline.DefaultPermissionCli.TestReadPermissionCommand;

@Cli(name = "@default-permission", commands = { TestNoPermissionCommand.class, TestReadPermissionCommand.class,
        TestAdminPermissionCommand.class })
@Permission(GHPermissionType.WRITE)
class DefaultPermissionCli {

    @Command(name = "test-no-permission")
    static class TestNoPermissionCommand implements PermissionCommand {

        @Override
        public void run(IssueComment issueCommentPayload) throws IOException {
            issueCommentPayload.getIssue().comment("hello from @default-permission test-no-permission");
        }
    }

    @Command(name = "test-read-permission")
    @Permission(GHPermissionType.READ)
    static class TestReadPermissionCommand implements PermissionCommand {

        @Override
        public void run(IssueComment issueCommentPayload) throws IOException {
            issueCommentPayload.getIssue().comment("hello from @default-permission test-read-permission");
        }
    }

    @Command(name = "test-admin-permission")
    @Permission(GHPermissionType.ADMIN)
    static class TestAdminPermissionCommand implements PermissionCommand {

        @Override
        public void run(IssueComment issueCommentPayload) throws IOException {
            issueCommentPayload.getIssue().comment("hello from @default-permission test-admin-permission");
        }
    }

    public interface PermissionCommand {

        void run(GHEventPayload.IssueComment issueCommentPayload) throws IOException;
    }
}
