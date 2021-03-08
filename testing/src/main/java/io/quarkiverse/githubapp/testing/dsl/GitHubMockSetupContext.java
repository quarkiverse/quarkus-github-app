package io.quarkiverse.githubapp.testing.dsl;

import java.io.IOException;

public interface GitHubMockSetupContext extends GitHubMockContext {

    void configFileFromClasspath(String pathInRepository, String pathInClassPath) throws IOException;

    void configFileFromString(String pathInRepository, String configFile);

}
