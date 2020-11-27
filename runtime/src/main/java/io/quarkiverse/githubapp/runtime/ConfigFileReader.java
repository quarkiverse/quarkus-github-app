package io.quarkiverse.githubapp.runtime;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.kohsuke.github.GHRepository;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkiverse.githubapp.runtime.UtilsProducer.Yaml;
import io.quarkiverse.githubapp.runtime.github.GitHubFileDownloader;

@RequestScoped
public class ConfigFileReader {

    private static final List<String> YAML_EXTENSIONS = Arrays.asList(".yml", ".yaml");
    private static final List<String> JSON_EXTENSIONS = Collections.singletonList(".json");
    private static final List<String> TEXT_EXTENSIONS = Collections.singletonList(".txt");

    private static final String DEFAULT_DIRECTORY = ".github/";
    private static final String PARENT_DIRECTORY = "..";
    private static final String ROOT_DIRECTORY = "/";

    private final Map<String, Object> cache = new ConcurrentHashMap<>();

    @Inject
    GitHubFileDownloader gitHubFileDownloader;

    @Inject
    ObjectMapper jsonObjectMapper;

    @Inject
    @Yaml
    ObjectMapper yamlObjectMapper;

    public Object getConfigObject(GHRepository ghRepository, String path, Class<?> type) {
        String fullPath = getFilePath(path.trim());
        String cacheKey = getCacheKey(ghRepository, fullPath);

        Object cachedObject = cache.get(cacheKey);
        if (cachedObject != null) {
            return cachedObject;
        }

        return cache.computeIfAbsent(cacheKey, k -> readConfigFile(ghRepository, fullPath, type));
    }

    private Object readConfigFile(GHRepository ghRepository, String fullPath, Class<?> type) {
        Optional<String> contentOptional = gitHubFileDownloader.getFileContent(ghRepository, fullPath);
        if (contentOptional.isEmpty()) {
            return null;
        }

        String content = contentOptional.get();

        if (matchExtensions(fullPath, TEXT_EXTENSIONS) && !String.class.equals(type)) {
            throw new IllegalArgumentException(
                    "Text extensions (" + String.join(", ", TEXT_EXTENSIONS) + ") only support String: " + fullPath
                            + " required type " + type.getName());
        }

        if (String.class.equals(type)) {
            return content;
        }

        try {
            ObjectMapper objectMapper = getObjectMapper(fullPath);
            return objectMapper.readValue(content, type);
        } catch (Exception e) {
            throw new IllegalStateException("Error deserializing config file " + fullPath + " to type " + type.getName(), e);
        }
    }

    private static String getCacheKey(GHRepository ghRepository, String fullPath) {
        // we should only handle the config files of one repository in a given ConfigFileReader
        // as it's request scoped but let's be on the safe side
        return ghRepository.getFullName() + ROOT_DIRECTORY + fullPath;
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

    private static String getFilePath(String path) {
        if (path.contains(PARENT_DIRECTORY)) {
            throw new IllegalArgumentException("Config file paths containing '..' are not accepted: " + path);
        }

        if (path.startsWith(ROOT_DIRECTORY)) {
            return path.substring(1);
        }

        return DEFAULT_DIRECTORY + path;
    }
}
