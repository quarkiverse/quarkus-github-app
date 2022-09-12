package io.quarkiverse.githubapp.runtime;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.kohsuke.github.GHRepository;

import io.quarkiverse.githubapp.ConfigFile;
import io.quarkiverse.githubapp.GitHubConfigFileProvider;
import io.quarkiverse.githubapp.runtime.config.GitHubAppRuntimeConfig;
import io.quarkiverse.githubapp.runtime.github.GitHubConfigFileProviderImpl;

@RequestScoped
public class RequestScopeCachingGitHubConfigFileProvider {

    @Inject
    GitHubAppRuntimeConfig gitHubAppRuntimeConfig;

    @Inject
    GitHubConfigFileProvider gitHubConfigFileProvider;

    private final Map<String, Object> cache = new ConcurrentHashMap<>();

    public Object getConfigObject(GHRepository ghRepository, String path, ConfigFile.Source source, Class<?> type) {
        String cacheKey = getCacheKey(ghRepository, path, source);

        Object cachedObject = cache.get(cacheKey);
        if (cachedObject != null) {
            return cachedObject;
        }

        return cache.computeIfAbsent(cacheKey,
                k -> gitHubConfigFileProvider.fetchConfigFile(ghRepository, path, source, type).orElse(null));
    }

    private String getCacheKey(GHRepository ghRepository, String path,
            ConfigFile.Source source) {
        String fullPath = GitHubConfigFileProviderImpl.getFilePath(path.trim());
        ConfigFile.Source effectiveSource = gitHubAppRuntimeConfig.getEffectiveSource(source);
        // we should only handle the config files of one repository in a given ConfigFileReader
        // as it's request scoped but let's be on the safe side
        return ghRepository.getFullName() + ":" + effectiveSource.name() + ":" + fullPath;
    }

}
