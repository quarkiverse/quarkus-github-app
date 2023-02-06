package io.quarkiverse.githubapp.runtime.github;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.kohsuke.github.GHRepository;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkiverse.githubapp.ConfigFile;
import io.quarkiverse.githubapp.GitHubConfigFileProvider;
import io.quarkiverse.githubapp.runtime.UtilsProducer;
import io.quarkiverse.githubapp.runtime.config.CheckedConfigProvider;

@ApplicationScoped
public class GitHubConfigFileProviderImpl implements GitHubConfigFileProvider {

    private static final List<String> YAML_EXTENSIONS = Arrays.asList(".yml", ".yaml");
    private static final List<String> JSON_EXTENSIONS = Collections.singletonList(".json");
    private static final List<String> TEXT_EXTENSIONS = Collections.singletonList(".txt");

    private static final String DEFAULT_DIRECTORY = ".github/";
    private static final String PARENT_DIRECTORY = "..";
    private static final String ROOT_DIRECTORY = "/";

    @Inject
    CheckedConfigProvider checkedConfigProvider;

    @Inject
    GitHubFileDownloader gitHubFileDownloader;

    @Inject
    ObjectMapper jsonObjectMapper;

    @Inject
    @UtilsProducer.Yaml
    ObjectMapper yamlObjectMapper;

    @Override
    public <T> Optional<T> fetchConfigFile(GHRepository repository, String path, ConfigFile.Source source, Class<T> type) {
        return fetchConfigFile(repository, null, path, source, type);
    }

    @Override
    public <T> Optional<T> fetchConfigFile(GHRepository repository, String ref, String path, ConfigFile.Source source,
            Class<T> type) {
        GHRepository configGHRepository = getConfigRepository(repository, source, path);

        String fullPath = getFilePath(path);

        Optional<String> contentOptional = gitHubFileDownloader.getFileContent(configGHRepository, ref, fullPath);
        if (contentOptional.isEmpty()) {
            return Optional.empty();
        }

        if (matchExtensions(fullPath, TEXT_EXTENSIONS) && !String.class.equals(type)) {
            throw new IllegalArgumentException(
                    "Text extensions (" + String.join(", ", TEXT_EXTENSIONS) + ") only support String: " + fullPath
                            + " required type " + type.getName());
        }

        if (String.class.equals(type)) {
            @SuppressWarnings("unchecked")
            Optional<T> result = (Optional<T>) contentOptional;
            return result;
        }

        try {
            ObjectMapper objectMapper = getObjectMapper(fullPath);
            return Optional.ofNullable(objectMapper.readValue(contentOptional.get(), type));
        } catch (Exception e) {
            throw new IllegalStateException("Error deserializing config file " + fullPath + " to type " + type.getName(), e);
        }
    }

    private GHRepository getConfigRepository(GHRepository ghRepository, ConfigFile.Source source, String path) {
        ConfigFile.Source effectiveSource = checkedConfigProvider.getEffectiveSource(source);

        if (effectiveSource == ConfigFile.Source.CURRENT_REPOSITORY) {
            return ghRepository;
        }
        if (!ghRepository.isFork()) {
            return ghRepository;
        }

        try {
            GHRepository sourceRepository = ghRepository.getSource();

            if (sourceRepository == null) {
                throw new IllegalStateException("Unable to get the source repository for fork " + ghRepository.getFullName()
                        + ": unable to read config file " + path);
            }

            return sourceRepository;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to get the source repository for fork " + ghRepository.getFullName()
                    + ": unable to read config file " + path, e);
        }
    }

    private ObjectMapper getObjectMapper(String path) {
        if (matchExtensions(path, YAML_EXTENSIONS)) {
            return yamlObjectMapper;
        }
        if (matchExtensions(path, JSON_EXTENSIONS)) {
            return jsonObjectMapper;
        }
        throw new IllegalArgumentException("File extension not supported for config file " + path);
    }

    private static boolean matchExtensions(String path, Collection<String> extensions) {
        for (String extension : extensions) {
            if (path.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    public static String getFilePath(String path) {
        String trimmedPath = path.trim();

        if (trimmedPath.contains(PARENT_DIRECTORY)) {
            throw new IllegalArgumentException("Config file paths containing '..' are not accepted: " + path);
        }

        if (trimmedPath.startsWith(ROOT_DIRECTORY)) {
            return path.substring(1);
        }

        return DEFAULT_DIRECTORY + trimmedPath;
    }
}
