package io.quarkiverse.githubapp;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Marks a parameter as a configuration file to be fetched from the code repository,
 * and optionally (if the parameter type is different from {@link String})
 * deserialized using Jackson.
 * <p>
 * Configuration files are always retrieved from the default branch,
 * but there is some flexibility as to which repository the files are retrieved from
 * in the case of forks, see {@link #source()}.
 */
@Target(PARAMETER)
@Retention(RUNTIME)
public @interface ConfigFile {

    /**
     * @return The path to the file in the code repository,
     * either absolute (if it starts with {@code /}) or relative to {@code /.github/} (if it doesn't start with {@code /}).
     */
    String value();

    /**
     * @return Which repository to extract the file from in the case of forked repositories.
     * @see Source
     */
    Source source() default Source.DEFAULT;

    /**
     * Which repository to extract a configuration file from.
     */
    enum Source {
        /**
         * Default behavior:
         * <ul>
         *     <li>If {@code quarkus.github-app.read-config-files-from-source-repository}
         *     is unset or set to {@code false}, behaves as {@link #CURRENT_REPOSITORY}</li>
         *     <li>If {@code quarkus.github-app.read-config-files-from-source-repository}
         *     is set to {@code true}, behaves as {@link #SOURCE_REPOSITORY}</li>
         * </ul>
         */
        DEFAULT,
        /**
         * Always retrieve the configuration file from the "source" (non-fork) repository;
         * in the case of forks, the configuration file living in the fork will be ignored.
         */
        SOURCE_REPOSITORY,
        /**
         * Always retrieve the configuration file from the repository from which an event was sent.
         */
        CURRENT_REPOSITORY;
    }
}