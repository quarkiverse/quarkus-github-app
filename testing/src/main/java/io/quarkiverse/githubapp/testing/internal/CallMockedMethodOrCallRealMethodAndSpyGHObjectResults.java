package io.quarkiverse.githubapp.testing.internal;

import java.io.Serializable;

import org.kohsuke.github.GHObject;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

final class CallMockedMethodOrCallRealMethodAndSpyGHObjectResults implements Answer<Object>, Serializable {

    private final CallRealMethodAndSpyGHObjectResults callRealMethodAndSpy;
    private final DefaultableMocking<? extends GHObject> ghObjectMocking;

    public CallMockedMethodOrCallRealMethodAndSpyGHObjectResults(CallRealMethodAndSpyGHObjectResults callRealMethodAndSpy,
            DefaultableMocking<? extends GHObject> ghObjectMocking) {
        this.callRealMethodAndSpy = callRealMethodAndSpy;
        this.ghObjectMocking = ghObjectMocking;
    }

    @Override
    public Object answer(InvocationOnMock invocation) throws Throwable {
        if (invocation.getMethod().getName().startsWith("get")) {
            return ghObjectMocking.callMockOrDefault(invocation, callRealMethodAndSpy);
        } else {
            return ghObjectMocking.callMock(invocation);
        }
    }
}
