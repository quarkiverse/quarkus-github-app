package io.quarkiverse.githubapp.testing.dsl;

import java.io.IOException;

import org.kohsuke.github.GHRepository;

import io.quarkiverse.githubapp.ConfigFile;

public interface GitHubMockSetupContext extends GitHubMockContext {

    /**
     * Mocks the fetching of a configuration file from the GitHub repository of an event,
     * using a file from the classpath as stubbed content.
     *
     * @param pathInRepository The path of the file passed to {@link ConfigFile#value()}.
     * @param pathInClasspath The path of the content to use in the classpath.
     * @throws IOException If reading the content from the classpath fails.
     * @see ConfigFile
     * @deprecated Call {@code configFile(pathInRepository).fromClasspath(pathInClassPath)} instead.
     *             Will be removed in version 2 of this extension.
     */
    @Deprecated(forRemoval = true)
    default void configFileFromClasspath(String pathInRepository, String pathInClasspath) throws IOException {
        configFile(pathInRepository).fromClasspath(pathInClasspath);
    }

    /**
     * Mocks the fetching of a configuration file from the GitHub repository of an event,
     * using a given string as stubbed content.
     *
     * @param pathInRepository The path of the file passed to {@link ConfigFile#value()}.
     * @param configFile The content of the configuration file as a string.
     * @see ConfigFile
     * @deprecated Call {@code configFile(pathInRepository).fromString(configFile)} instead.
     *             Will be removed in version 2 of this extension.
     */
    @Deprecated
    default void configFileFromString(String pathInRepository, String configFile) {
        configFile(pathInRepository).fromString(configFile);
    }

    /**
     * Starts mocking the fetching of a configuration file from the GitHub repository of an event.
     *
     * @param pathInRepository The path of the file passed to {@link ConfigFile#value()}.
     * @return A context to set the stubbed content of the file.
     * @see ConfigFile
     */
    GitHubMockConfigFileSetupContext configFile(String pathInRepository);

    /**
     * Starts mocking the fetching of a configuration file from a given GitHub repository mock.
     *
     * @param repository The repository mock, generally retrieved from {@link #repository(String)}.
     * @param pathInRepository The path of the file passed to
     *        {@link io.quarkiverse.githubapp.GitHubConfigFileProvider#fetchConfigFile(GHRepository, String, ConfigFile.Source, Class)}.
     * @see GitHubMockContext#repository(String)
     * @see io.quarkiverse.githubapp.GitHubConfigFileProvider#fetchConfigFile(GHRepository, String, ConfigFile.Source, Class)
     */
    GitHubMockConfigFileSetupContext configFile(GHRepository repository, String pathInRepository);

}
