package io.quarkiverse.githubapp.testing.dsl;

import java.io.IOException;

public interface GitHubMockConfigFileSetupContext {

    /**
     * Finalizes mocking, using a file from the classpath as stubbed content.
     *
     * @param pathInClasspath The path of the content to use in the classpath.
     * @throws IOException If reading the content from the classpath fails.
     */
    void fromClasspath(String pathInClasspath) throws IOException;

    /**
     * Finalizes mocking, using a given string as stubbed content.
     *
     * @param configFile The content of the configuration file as a string.
     */
    void fromString(String configFile);

}
