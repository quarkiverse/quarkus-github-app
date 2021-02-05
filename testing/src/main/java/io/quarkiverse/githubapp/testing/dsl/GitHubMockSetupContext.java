package io.quarkiverse.githubapp.testing.dsl;

import java.io.IOException;

public interface GitHubMockSetupContext extends GitHubMockContext {

    <T> void configFileFromClasspath(String pathInRepository, String pathInClassPath) throws IOException;

    <T> void configFileFromString(String pathInRepository, String configFile);

}
