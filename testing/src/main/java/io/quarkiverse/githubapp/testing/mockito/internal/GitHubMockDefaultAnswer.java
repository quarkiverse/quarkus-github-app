package io.quarkiverse.githubapp.testing.mockito.internal;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.function.Function;

import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * The default answer for all {@link GitHub} mocks.
 * <p>
 * The purpose of this default answer is to control the behavior of a few special methods:
 * <ul>
 * <li>package-protected methods such as {@code GitHub#intern(GHUser)},
 * whose stubbing would normally require relying on external help such as Powermock.</li>
 * <li>other methods that need to be stubbed but cannot,
 * because they are not guaranteed to be called and thus might trigger an
 * {@link org.mockito.exceptions.misusing.UnnecessaryStubbingException}
 * in {@link org.mockito.Mock.Strictness#STRICT_STUBS} mode.
 * (example: {@link GitHub#isOffline()})</li>
 * </ul>
 */
public final class GitHubMockDefaultAnswer implements Answer<Object>, Serializable {

    private final Answer<Object> delegate;
    private final Function<String, GHRepository> repositoryMockProvider;

    public GitHubMockDefaultAnswer(Answer<Object> delegate, Function<String, GHRepository> repositoryMockProvider) {
        this.delegate = delegate;
        this.repositoryMockProvider = repositoryMockProvider;
    }

    @Override
    public Object answer(InvocationOnMock invocation) throws Throwable {
        Method method = invocation.getMethod();
        if (method.getParameterCount() == 1) {
            switch (method.getName()) {
                case "intern":
                    return invocation.callRealMethod();
                case "isOffline":
                    // Stubbed GitHub clients are always offline
                    return true;
                case "getRepository":
                    return repositoryMockProvider.apply(invocation.getArgument(0, String.class));
            }
        }
        return delegate.answer(invocation);
    }
}
