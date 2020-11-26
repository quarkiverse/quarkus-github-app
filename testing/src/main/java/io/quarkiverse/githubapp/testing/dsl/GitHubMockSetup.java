package io.quarkiverse.githubapp.testing.dsl;

@FunctionalInterface
public interface GitHubMockSetup<T extends Throwable> {

    void setup(GitHubMockSetupContext mocks) throws T;
    
}
