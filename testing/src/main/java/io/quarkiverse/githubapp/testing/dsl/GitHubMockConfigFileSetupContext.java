package io.quarkiverse.githubapp.testing.dsl;

import java.io.IOException;

import org.kohsuke.github.GHRepository;

import io.quarkiverse.githubapp.ConfigFile;

public interface GitHubMockConfigFileSetupContext {

    /**
     * Mocks the retrieval of particular ref.
     *
     * @param ref The ref passed to
     *        {@link io.quarkiverse.githubapp.GitHubConfigFileProvider#fetchConfigFile(GHRepository, String, String, ConfigFile.Source, Class)}
     * @return {@code this}, to continue mocking setup.
     * @see io.quarkiverse.githubapp.GitHubConfigFileProvider#fetchConfigFile(GHRepository, String, String, ConfigFile.Source,
     *      Class)
     */
    GitHubMockConfigFileSetupContext withRef(String ref);

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
