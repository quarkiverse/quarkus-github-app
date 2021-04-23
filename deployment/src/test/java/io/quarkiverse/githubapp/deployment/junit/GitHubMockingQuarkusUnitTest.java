package io.quarkiverse.githubapp.deployment.junit;

import java.util.function.Supplier;

import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.extension.ExtensionContext;

import io.quarkus.test.QuarkusUnitTest;

public class GitHubMockingQuarkusUnitTest extends QuarkusUnitTest {

    @Override
    public GitHubMockingQuarkusUnitTest setArchiveProducer(Supplier<JavaArchive> archiveProducer) {
        super.setArchiveProducer(() -> archiveProducer.get()
                .addClass(CallMockedMethodOrCallRealMethodAndSpyGHObjectResults.class)
                .addClass(CallRealMethodAndSpyGHObjectResults.class)
                .addClass(DefaultableMocking.class)
                .addClass(GitHubMockContextImpl.class)
                .addClass(GitHubMockingQuarkusUnitTest.class)
                .addClass(GitHubMocks.class)
                .addClass(QuarkusMock.class)
                .addClass(MockSupport.class));
        return this;
    }

    @Override
    public GitHubMockingQuarkusUnitTest withConfigurationResource(String resourceName) {
        super.withConfigurationResource(resourceName);
        return this;
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        super.beforeAll(extensionContext);
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
        super.afterAll(extensionContext);
    }
}
