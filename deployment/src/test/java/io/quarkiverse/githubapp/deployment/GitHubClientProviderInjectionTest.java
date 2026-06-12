package io.quarkiverse.githubapp.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.githubapp.GitHubClientProvider;
import io.quarkiverse.githubapp.runtime.github.GitHubService;
import io.quarkus.arc.Arc;
import io.quarkus.test.QuarkusExtensionTest;

public class GitHubClientProviderInjectionTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class))
            .withConfigurationResource("application.properties");

    @Test
    public void test() {
        assertThat(Arc.container().instance(GitHubClientProvider.class).get())
                .isInstanceOf(GitHubService.class);
    }
}
