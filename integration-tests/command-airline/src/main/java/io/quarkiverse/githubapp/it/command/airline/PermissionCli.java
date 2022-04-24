package io.quarkiverse.githubapp.it.command.airline;

import java.io.IOException;

import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHEventPayload.IssueComment;
import org.kohsuke.github.GHPermissionType;

import com.github.rvesse.airline.annotations.Cli;
import com.github.rvesse.airline.annotations.Command;

import io.quarkiverse.githubapp.command.airline.Permission;
import io.quarkiverse.githubapp.it.command.airline.PermissionCli.TestAdminPermissionCommand;
import io.quarkiverse.githubapp.it.command.airline.PermissionCli.TestNoPermissionCommand;
import io.quarkiverse.githubapp.it.command.airline.PermissionCli.TestWritePermissionCommand;

@Cli(name = "@permission", commands = { TestNoPermissionCommand.class, TestWritePermissionCommand.class,
        TestAdminPermissionCommand.class })
class PermissionCli {

    @Command(name = "test-no-permission")
    static class TestNoPermissionCommand implements PermissionCommand {

        @Override
        public void run(IssueComment issueCommentPayload) throws IOException {
            issueCommentPayload.getIssue().comment("hello from @permission test-no-permission");
        }
    }

    @Command(name = "test-write-permission")
    @Permission(GHPermissionType.WRITE)
    static class TestWritePermissionCommand implements PermissionCommand {

        @Override
        public void run(IssueComment issueCommentPayload) throws IOException {
            issueCommentPayload.getIssue().comment("hello from @permission test-write-permission");
        }
    }

    @Command(name = "test-admin-permission")
    @Permission(GHPermissionType.ADMIN)
    static class TestAdminPermissionCommand implements PermissionCommand {

        @Override
        public void run(IssueComment issueCommentPayload) throws IOException {
            issueCommentPayload.getIssue().comment("hello from @permission test-admin-permission");
        }
    }

    public interface PermissionCommand {

        void run(GHEventPayload.IssueComment issueCommentPayload) throws IOException;
    }
}
