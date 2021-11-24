package io.quarkiverse.githubapp.runtime.graal;

import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * We drop this connector altogether as the OkHttp version we are using is not compatible with so we can't build the native
 * executable with it around.
 * <p>
 * One option to fix it is to force using a newer version in each GitHub App project but it's not very practical, especially
 * since we are now using OkHttp just for the event source in dev mode.
 */
@TargetClass(className = "org.kohsuke.github.extras.okhttp3.OkHttpGitHubConnector")
@Delete
public final class Delete_OkHttpGitHubConnector {

}
