package io.quarkiverse.githubapp.it.command.airline;

import java.io.IOException;

import org.kohsuke.github.GHEventPayload;

import com.github.rvesse.airline.annotations.Cli;
import com.github.rvesse.airline.annotations.Command;

import io.quarkiverse.githubapp.ConfigFile;
import io.quarkiverse.githubapp.it.command.airline.ConfigFileReaderCli.TestCommand;

@Cli(name = "@config-file-reader", commands = { TestCommand.class })
public class ConfigFileReaderCli {

    @Command(name = "test")
    static class TestCommand implements ConfigFileReaderCommand {

        @Override
        public void run(GHEventPayload.IssueComment issueCommentPayload, MyConfigBean myConfigBean) throws IOException {
            issueCommentPayload.getIssue().comment(myConfigBean.getHello());
        }
    }

    public interface ConfigFileReaderCommand {

        void run(GHEventPayload.IssueComment issueCommentPayload,
                @ConfigFile("config-file-reader.yml") MyConfigBean myConfigBean) throws IOException;
    }

    public static class MyConfigBean {

        private String hello;

        public String getHello() {
            return hello;
        }
    }
}
