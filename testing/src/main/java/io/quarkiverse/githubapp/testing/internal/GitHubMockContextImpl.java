package io.quarkiverse.githubapp.testing.internal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHObject;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.mockito.Mockito;

import io.quarkiverse.githubapp.runtime.github.GitHubFileDownloader;
import io.quarkiverse.githubapp.runtime.github.GitHubService;
import io.quarkiverse.githubapp.testing.dsl.GitHubMockContext;
import io.quarkiverse.githubapp.testing.dsl.GitHubMockSetupContext;
import io.quarkiverse.githubapp.testing.dsl.GitHubMockVerificationContext;

public final class GitHubMockContextImpl implements GitHubMockContext, GitHubMockSetupContext, GitHubMockVerificationContext {

    final GitHubService service;
    final GitHubFileDownloader fileDownloader;

    private final List<MockMap<?, ?>> allMockMaps = new ArrayList<>();
    private final MockMap<Long, GitHub> clients = new MockMap<>(GitHub.class);
    private final MockMap<String, GHRepository> repositories = new MockMap<>(GHRepository.class);
    private final Map<Class<?>, MockMap<Long, ? extends GHObject>> nonRepositoryGHObjectMockMaps = new LinkedHashMap<>();

    GitHubMockContextImpl() {
        fileDownloader = Mockito.mock(GitHubFileDownloader.class);
        service = Mockito.mock(GitHubService.class);
    }

    @Override
    public GitHub client(long id) {
        return clients.getOrCreate(id, newClient -> {
            try {
                when(newClient.getRepository(any()))
                        .thenAnswer(invocation -> repository(invocation.getArgument(0, String.class)));
                when(newClient.parseEventPayload(any(), any())).thenAnswer(invocation -> {
                    Object original = invocation.callRealMethod();
                    return Mockito.mock(original.getClass(), withSettings().spiedInstance(original)
                            .defaultAnswer(new CallRealMethodAndSpyGHObjectResults(this)));
                });
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        })
                .mock();
    }

    @Override
    public <T> void configFileFromClasspath(String pathInRepository, String pathInClassPath) throws IOException {
        configFileFromString(pathInRepository, GitHubAppTestingContext.get().getFromClasspath(pathInClassPath));
    }

    @Override
    public <T> void configFileFromString(String pathInRepository, String configFile) {
        when(fileDownloader.getFileContent(any(), eq(getGitHubFilePath(pathInRepository))))
                .thenReturn(Optional.of(configFile));
    }

    @Override
    public GHRepository repository(String id) {
        return repositories.getOrCreate(id).mock();
    }

    @Override
    public GHIssue issue(long id) {
        return nonRepositoryMockMap(GHIssue.class).getOrCreate(id).mock();
    }

    @Override
    public GHPullRequest pullRequest(long id) {
        return nonRepositoryMockMap(GHPullRequest.class).getOrCreate(id).mock();
    }

    @Override
    public Object[] ghObjects() {
        List<GHObject> result = new ArrayList<>();
        for (MockMap<?, ?> mockMap : allMockMaps) {
            if (!GHObject.class.isAssignableFrom(mockMap.clazz)) {
                continue;
            }
            for (DefaultableMocking<?> mocking : mockMap.map.values()) {
                result.add((GHObject) mocking.mock());
            }
        }
        return result.toArray();
    }

    void init() {
        reset();

        when(service.getInstallationClient(any()))
                .thenAnswer(invocation -> client(invocation.getArgument(0, Long.class)));
    }

    void reset() {
        Mockito.reset(service);
        Mockito.reset(fileDownloader);
        for (MockMap<?, ?> mockMap : allMockMaps) {
            mockMap.map.clear();
        }
    }

    private static String getGitHubFilePath(String path) {
        if (path.startsWith("/")) {
            return path.substring(1);
        }

        return ".github/" + path;
    }

    DefaultableMocking<? extends GHObject> ghObjectMocking(GHObject original) {
        Class<? extends GHObject> type = original.getClass();
        if (GHRepository.class.equals(type)) {
            return repositories.getOrCreate(((GHRepository) original).getName());
        } else {
            return nonRepositoryMockMap(type).getOrCreate(original.getId());
        }
    }

    @SuppressWarnings("unchecked")
    <T extends GHObject> MockMap<Long, T> nonRepositoryMockMap(Class<T> type) {
        if (GHRepository.class.equals(type)) {
            throw new IllegalArgumentException("Type must not be GHRepository -- there is a bug in the testing helper.");
        }
        return (MockMap<Long, T>) nonRepositoryGHObjectMockMaps.computeIfAbsent(type, clazz -> new MockMap<>(type));
    }

    private final class MockMap<ID, T> {

        private final Class<T> clazz;
        private final Map<ID, DefaultableMocking<T>> map = new LinkedHashMap<>();

        private MockMap(Class<T> clazz) {
            this.clazz = clazz;
            GitHubMockContextImpl.this.allMockMaps.add(this);
        }

        private DefaultableMocking<T> getOrCreate(ID id) {
            return map.computeIfAbsent(id, this::create);
        }

        private DefaultableMocking<T> getOrCreate(ID id, Consumer<T> consumerIfCreated) {
            return map.computeIfAbsent(id, theId -> {
                DefaultableMocking<T> result = create(theId);
                consumerIfCreated.accept(result.mock());
                return result;
            });
        }

        private DefaultableMocking<T> create(Object id) {
            return DefaultableMocking.create(clazz, id);
        }
    }
}
