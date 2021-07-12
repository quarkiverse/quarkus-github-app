package io.quarkiverse.githubapp.testing.mockito.internal;

import static org.mockito.Mockito.withSettings;

import java.io.Serializable;
import java.util.function.Function;

import org.kohsuke.github.GHObject;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public final class CallRealMethodAndSpyGHObjectResults implements Answer<Object>, Serializable {

    private final Function<GHObject, DefaultableMocking<? extends GHObject>> defaultableMockingProvider;

    public CallRealMethodAndSpyGHObjectResults(
            Function<GHObject, DefaultableMocking<? extends GHObject>> defaultableMockingProvider) {
        this.defaultableMockingProvider = defaultableMockingProvider;
    }

    @Override
    public Object answer(InvocationOnMock invocation) throws Throwable {
        Object original = invocation.callRealMethod();
        if (!(original instanceof GHObject)) {
            return original;
        }
        GHObject castOriginal = (GHObject) original;
        Class<? extends GHObject> type = castOriginal.getClass();
        DefaultableMocking<? extends GHObject> mocking = defaultableMockingProvider.apply(castOriginal);
        return Mockito.mock(type, withSettings().stubOnly()
                .withoutAnnotations()
                .spiedInstance(original)
                .defaultAnswer(new CallMockedMethodOrCallRealMethodAndSpyGHObjectResults(this, mocking)));
    }

}
