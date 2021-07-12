package io.quarkiverse.githubapp.it.testingframework;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.arc.Arc;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
//@GithubAppTest // NOT using the testing framework on purpose
public class NoTestingFrameworkTest {

    @Test
    void checkApplicationIncludesListeners() {
        // We just want to check that:
        // 1. The application started and includes our listeners
        // 2. This test works even though we are not using @GithubAppTest (because we're not using the testing framework)
        assertThat(Arc.container().instance(IssueEventListener.class))
                .isNotNull();
        assertThat(Arc.container().instance(PullRequestEventListener.class))
                .isNotNull();
    }

}
