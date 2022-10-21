package io.quarkiverse.githubapp.testing.internal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.kohsuke.github.AbuseLimitHandler;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHObject;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHTeam;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.HttpConnector;
import org.kohsuke.github.RateLimitHandler;
import org.kohsuke.github.authorization.AuthorizationProvider;
import org.kohsuke.github.internal.GitHubConnectorHttpConnectorAdapter;
import org.mockito.Answers;
import org.mockito.MockSettings;
import org.mockito.Mockito;

import io.quarkiverse.githubapp.runtime.github.GitHubConfigFileProviderImpl;
import io.quarkiverse.githubapp.runtime.github.GitHubFileDownloader;
import io.quarkiverse.githubapp.runtime.github.GitHubService;
import io.quarkiverse.githubapp.testing.dsl.GitHubMockConfigFileSetupContext;
import io.quarkiverse.githubapp.testing.dsl.GitHubMockContext;
import io.quarkiverse.githubapp.testing.dsl.GitHubMockSetupContext;
import io.quarkiverse.githubapp.testing.dsl.GitHubMockVerificationContext;
import io.quarkiverse.githubapp.testing.mockito.internal.DefaultableMocking;
import io.quarkiverse.githubapp.testing.mockito.internal.GHEventPayloadSpyDefaultAnswer;
import io.quarkiverse.githubapp.testing.mockito.internal.GitHubMockDefaultAnswer;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClient;

public final class GitHubMockContextImpl implements GitHubMockContext, GitHubMockSetupContext, GitHubMockVerificationContext {

    final GitHubService service;
    final GitHubFileDownloader fileDownloader;

    private final List<MockMap<?, ?>> allMockMaps = new ArrayList<>();
    private final MockMap<Long, GitHub> clients;
    private final MockMap<Object, DynamicGraphQLClient> graphQLClients;
    private final MockMap<String, GHRepository> repositories;
    private final Map<Class<?>, MockMap<Long, ? extends GHObject>> nonRepositoryGHObjectMockMaps = new LinkedHashMap<>();
    private final Answers defaultAnswers;

    GitHubMockContextImpl(Answers defaultAnswers) {
        this.defaultAnswers = defaultAnswers;
        fileDownloader = MockitoUtils.doWithMockedClassClassLoader(GitHubFileDownloader.class,
                () -> Mockito.mock(GitHubFileDownloader.class));
        service = MockitoUtils.doWithMockedClassClassLoader(GitHubFileDownloader.class,
                () -> Mockito.mock(GitHubService.class, withSettings().stubOnly()));
        repositories = new MockMap<>(GHRepository.class);
        clients = new MockMap<>(GitHub.class,
                // Configure the client mocks to be offline, because we don't want to send HTTP requests.
                settings -> settings.useConstructor("https://api.github.invalid",
                        new GitHubConnectorHttpConnectorAdapter(HttpConnector.OFFLINE), RateLimitHandler.WAIT,
                        AbuseLimitHandler.WAIT, null, AuthorizationProvider.ANONYMOUS)
                        .defaultAnswer(new GitHubMockDefaultAnswer(defaultAnswers, this::repository)));
        graphQLClients = new MockMap<>(DynamicGraphQLClient.class);
    }

    @Override
    public GitHub applicationClient() {
        return applicationOrInstallationClient(null);
    }

    @Override
    public GitHub installationClient(long installationId) {
        return applicationOrInstallationClient(installationId);
    }

    // By convention, the application client has a null ID.
    public GitHub applicationOrInstallationClient(Long idOrNull) {
        return clients.getOrCreate(idOrNull).mock();
    }

    @Override
    public DynamicGraphQLClient installationGraphQLClient(long installationId) {
        return graphQLClients.getOrCreate(installationId)
                .mock();
    }

    @Override
    public GitHubMockConfigFileSetupContext configFile(String pathInRepository) {
        return configFile(null, pathInRepository);
    }

    @Override
    public GitHubMockConfigFileSetupContext configFile(GHRepository repository, String pathInRepository) {
        return new GitHubMockConfigFileSetupContext() {

            private String ref;

            @Override
            public GitHubMockConfigFileSetupContext withRef(String ref) {
                this.ref = ref;
                return this;
            }

            @Override
            public void fromClasspath(String pathInClasspath) throws IOException {
                fromString(GitHubAppTestingContext.get().getFromClasspath(pathInClasspath));
            }

            @Override
            public void fromString(String configFile) {
                when(fileDownloader.getFileContent(repository == null ? any() : same(repository),
                        ref == null ? isNull() : eq(ref), eq(GitHubConfigFileProviderImpl.getFilePath(pathInRepository))))
                        .thenReturn(Optional.of(configFile));
            }
        };
    }

    @Override
    public GHRepository repository(String fullName) {
        return repositories.getOrCreate(fullName).mock();
    }

    @Override
    public GHIssue issue(long id) {
        return ghObject(GHIssue.class, id);
    }

    @Override
    public GHPullRequest pullRequest(long id) {
        return ghObject(GHPullRequest.class, id);
    }

    @Override
    public GHIssueComment issueComment(long id) {
        return ghObject(GHIssueComment.class, id);
    }

    @Override
    public GHTeam team(long id) {
        return ghObject(GHTeam.class, id);
    }

    @Override
    public <T extends GHObject> T ghObject(Class<T> type, long id) {
        return nonRepositoryMockMap(type).getOrCreate(id).mock();
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

        when(service.getApplicationClient())
                .thenAnswer(invocation -> applicationClient());

        when(service.getInstallationClient(anyLong()))
                .thenAnswer(invocation -> installationClient(invocation.getArgument(0, Long.class)));

        when(service.getInstallationGraphQLClient(anyLong()))
                .thenAnswer(invocation -> installationGraphQLClient(invocation.getArgument(0, Long.class)));
    }

    void initEventStubs(long installationId) {
        GitHub clientMock = installationClient(installationId);
        MockitoUtils.doWithMockedClassClassLoader(GitHub.class, () -> {
            try {
                when(clientMock.parseEventPayload(any(), any())).thenAnswer(invocation -> {
                    Object original = invocation.callRealMethod();
                    return Mockito.mock(original.getClass(), withSettings().spiedInstance(original)
                            .withoutAnnotations()
                            .defaultAnswer(new GHEventPayloadSpyDefaultAnswer(clientMock, this::ghObjectMocking)));
                });
            } catch (RuntimeException | IOException e) {
                throw new AssertionError("Stubbing before the simulated event threw an exception: " + e.getMessage(), e);
            }
        });
    }

    void reset() {
        Mockito.reset(service);
        Mockito.reset(fileDownloader);
        for (MockMap<?, ?> mockMap : allMockMaps) {
            mockMap.map.clear();
        }
    }

    private DefaultableMocking<? extends GHObject> ghObjectMocking(GHObject original) {
        Class<? extends GHObject> type = original.getClass();
        if (GHRepository.class.equals(type)) {
            return repositories.getOrCreate(((GHRepository) original).getFullName());
        } else {
            return nonRepositoryMockMap(type).getOrCreate(original.getId());
        }
    }

    @SuppressWarnings("unchecked")
    <T extends GHObject> MockMap<Long, T> nonRepositoryMockMap(Class<T> type) {
        if (GHRepository.class.equals(type)) {
            throw new IllegalArgumentException("Type must not be GHRepository -- there is a bug in the testing helper.");
        }
        return (MockMap<Long, T>) nonRepositoryGHObjectMockMaps.computeIfAbsent(type,
                clazz -> new MockMap<>(type));
    }

    private final class MockMap<ID, T> {

        private final Class<T> clazz;
        private final Consumer<MockSettings> mockSettingsContributor;
        private final Map<ID, DefaultableMocking<T>> map = new LinkedHashMap<>();

        private MockMap(Class<T> clazz) {
            this(clazz, mockSettings -> {
            });
        }

        private MockMap(Class<T> clazz, Consumer<MockSettings> mockSettingsContributor) {
            this.clazz = clazz;
            this.mockSettingsContributor = mockSettings -> {
                mockSettings.defaultAnswer(defaultAnswers);
                mockSettingsContributor.accept(mockSettings);
            };
            GitHubMockContextImpl.this.allMockMaps.add(this);
        }

        private DefaultableMocking<T> getOrCreate(ID id) {
            return map.computeIfAbsent(id, this::create);
        }

        private DefaultableMocking<T> create(Object id) {
            return MockitoUtils.doWithMockedClassClassLoader(clazz,
                    () -> DefaultableMocking.create(clazz, id, mockSettingsContributor));
        }
    }
}
