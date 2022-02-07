package io.quarkiverse.githubapp.testing.mockito.internal;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.kohsuke.github.GHObject;
import org.kohsuke.github.GitHub;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * The default answer for all {@link GHObject} spies.
 * <p>
 * The purpose of this default answer is to apply mocked behavior when defined
 * (through {@link io.quarkiverse.githubapp.testing.dsl.GitHubMockContext}),
 * or failing that retrieve information from the event payload if possible (e.g. for getters),
 * or failing that apply whatever default behavior is configured globally (e.g. return {@code null}, ...).
 * <p>
 * For {@code GHObject#root()} and {@link GHObject#getRoot()}, this will return the {@link GitHub} client mock.
 * <p>
 * For other getters, if there is a mocked behavior, this will apply that behavior.
 * Otherwise, this will call the real method,
 * and potentially wrap the return value with a spy using {@link GHObjectSpyDefaultAnswer},
 * if that return value is a {@link GHObject}.
 * <p>
 * For all other methods, this will just call the mocked behavior.
 */
public final class GHObjectSpyDefaultAnswer implements Answer<Object>, Serializable {

    private final GitHub clientSpy;
    private final GHEventPayloadSpyDefaultAnswer callRealMethodAndSpy;
    private final DefaultableMocking<? extends GHObject> ghObjectMocking;

    public GHObjectSpyDefaultAnswer(GitHub clientSpy,
            GHEventPayloadSpyDefaultAnswer callRealMethodAndSpy,
            DefaultableMocking<? extends GHObject> ghObjectMocking) {
        this.clientSpy = clientSpy;
        this.callRealMethodAndSpy = callRealMethodAndSpy;
        this.ghObjectMocking = ghObjectMocking;
    }

    @Override
    public Object answer(InvocationOnMock invocation) throws Throwable {
        Method method = invocation.getMethod();
        if (method.getParameterCount() == 0 && (method.getName().equals("root") || method.getName().equals("getRoot"))) {
            return clientSpy;
        } else if (method.getName().startsWith("get")) {
            return ghObjectMocking.callMockOrDefault(invocation, callRealMethodAndSpy);
        } else {
            return ghObjectMocking.callMock(invocation);
        }
    }
}
