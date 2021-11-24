package io.quarkiverse.githubapp.runtime.graal;

import org.kohsuke.github.connector.GitHubConnector;
import org.kohsuke.github.extras.HttpClientGitHubConnector;
import org.kohsuke.github.internal.DefaultGitHubConnector;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * Force the Java 11 HTTP client usage.
 */
@TargetClass(DefaultGitHubConnector.class)
public final class Substitute_DefaultGitHubConnector {

    @Substitute
    static GitHubConnector create(String defaultConnectorProperty) {
        return new HttpClientGitHubConnector();
    }
}
