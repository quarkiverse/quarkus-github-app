package io.quarkiverse.githubapp.deployment.junit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Singleton;

import io.quarkiverse.githubapp.runtime.github.GitHubFileDownloader;
import io.quarkiverse.githubapp.runtime.github.GitHubService;
import io.quarkus.runtime.Startup;

@Singleton
@Startup
public class GitHubMocks {

    private GitHubMockContextImpl mockContext;

    @PostConstruct
    public void init() {
        mockContext = new GitHubMockContextImpl();
        mockContext.init();

        MockSupport.pushContext();
        QuarkusMock.installMockForType(mockContext.service, GitHubService.class);
        QuarkusMock.installMockForType(mockContext.fileDownloader, GitHubFileDownloader.class);
    }

    @PreDestroy
    public void destroy() {
        if (mockContext != null) {
            mockContext.reset();
        }
        MockSupport.popContext();
    }

    public GitHubMockContextImpl getMockContext() {
        return mockContext;
    }
}
