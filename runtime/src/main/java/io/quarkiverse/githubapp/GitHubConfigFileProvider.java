package io.quarkiverse.githubapp;

import java.util.Optional;

import org.kohsuke.github.GHRepository;

/**
 * A provider of configuration files fetched from {@link org.kohsuke.github.GHRepository GitHub repositories}.
 * <p>
 * Inject as a CDI bean.
 * <p>
 * <strong>NOTE:</strong> You generally will not need this bean when processing events,
 * as configuration files can be automatically injected into event listener methods,
 * simply by annotating a parameter with {@link ConfigFile}.
 * This provider is mostly useful for non-event use cases (e.g. cron jobs).
 *
 * @see ConfigFile
 * @see ConfigFile.Source
 */
public interface GitHubConfigFileProvider {

    /**
     * Fetches the configuration file at the given path from the main branch of the given repository,
     * optionally (if {@code type} is not just {@link String}) deserializing it to the given type using Jackson.
     * <p>
     * <strong>NOTE:</strong> You generally will not need this method when processing events,
     * as configuration files can be automatically injected into event listener methods,
     * simply by annotating a parameter with {@link ConfigFile}.
     * This provider is mostly useful for non-event use cases (e.g. cron jobs).
     *
     * @param repository The GitHub code repository to retrieve the file from.
     * @param path The path to the file in the code repository,
     *        either absolute (if it starts with {@code /}) or relative to {@code /.github/} (if it doesn't start with
     *        {@code /}).
     * @param source Which repository to extract the file from in the case of forked repositories.
     * @param type The type to deserialize the file to.
     * @return The configuration file wrapped in an {@link java.util.Optional}, or {@link Optional#empty()} if it is missing.
     * @throws java.io.IOException If the configuration file cannot be retrieved.
     * @throws IllegalStateException If the configuration file cannot be deserialized to the given type.
     * @see ConfigFile
     * @see ConfigFile.Source
     */
    <T> Optional<T> fetchConfigFile(GHRepository repository, String path, ConfigFile.Source source, Class<T> type);

}
