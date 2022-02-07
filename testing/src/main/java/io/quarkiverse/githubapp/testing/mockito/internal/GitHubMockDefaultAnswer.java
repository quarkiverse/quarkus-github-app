package io.quarkiverse.githubapp.testing.mockito.internal;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * The default answer for all {@link GitHub} mocks.
 * <p>
 * The purpose of this default answer is to control the behavior of package-protected methods
 * such as {@code GitHub#intern(GHUser)},
 * without relying on external help such as Powermock.
 */
public final class GitHubMockDefaultAnswer implements Answer<Object>, Serializable {

    private final Answer<Object> delegate;

    public GitHubMockDefaultAnswer(Answer<Object> delegate) {
        this.delegate = delegate;
    }

    @Override
    public Object answer(InvocationOnMock invocation) throws Throwable {
        Method method = invocation.getMethod();
        if (method.getParameterCount() == 1 && method.getName().equals("intern")) {
            return invocation.callRealMethod();
        } else {
            return delegate.answer(invocation);
        }
    }
}
