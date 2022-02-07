package io.quarkiverse.githubapp.testing.mockito.internal;

import static org.mockito.Mockito.withSettings;

import java.io.Serializable;
import java.util.function.Function;

import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHObject;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * The default answer for all {@link GHEventPayload} spies.
 * <p>
 * The purpose of this default answer is to retrieve information from the event payload if possible (e.g. for getters),
 * or failing that apply whatever default behavior is configured globally (e.g. return {@code null}, ...).
 * <p>
 * This will call the real method,
 * and potentially wrap the return value with a spy using {@link GHObjectSpyDefaultAnswer},
 * if that return value is a {@link GHObject}.
 */
public final class GHEventPayloadSpyDefaultAnswer implements Answer<Object>, Serializable {

    private final Function<GHObject, DefaultableMocking<? extends GHObject>> defaultableMockingProvider;

    public GHEventPayloadSpyDefaultAnswer(
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
                .defaultAnswer(new GHObjectSpyDefaultAnswer(this, mocking)));
    }

}
