package io.quarkiverse.githubapp.testing.mockito.internal;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.function.Consumer;

import org.mockito.Answers;
import org.mockito.MockSettings;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.listeners.InvocationListener;
import org.mockito.listeners.MethodInvocationReport;
import org.mockito.stubbing.Answer;

public final class DefaultableMocking<M> {

    public static <M> DefaultableMocking<M> create(Class<M> clazz, Object id, Consumer<MockSettings> mockSettingsContributor,
            Answers defaultAnswer) {
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(clazz.getClassLoader());
            StubDetectingInvocationListener listener = new StubDetectingInvocationListener();
            MockSettings mockSettings = Mockito.withSettings().name(clazz.getSimpleName() + "#" + id)
                    .withoutAnnotations()
                    .defaultAnswer(defaultAnswer)
                    .invocationListeners(listener);
            mockSettingsContributor.accept(mockSettings);
            M mock = Mockito.mock(clazz, mockSettings);
            return new DefaultableMocking<>(mock, listener);
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
    }

    private final M mock;
    private final StubDetectingInvocationListener listener;

    private DefaultableMocking(M mock, StubDetectingInvocationListener listener) {
        this.mock = mock;
        this.listener = listener;
    }

    public M mock() {
        return mock;
    }

    Object callMock(InvocationOnMock invocation) throws Throwable {
        return call(mock, invocation);
    }

    Object callMockOrDefault(InvocationOnMock invocation, Answer<?> defaultAnswer) throws Throwable {
        Object result = callMock(invocation);
        if (listener.lastInvocationWasMocked) {
            return result;
        } else {
            call(Mockito.verify(mock, Mockito.atLeastOnce()), invocation);
            return defaultAnswer.answer(invocation);
        }
    }

    Object call(Object self, InvocationOnMock invocation) throws Throwable {
        Object[] argumentsForJava = unexpandArguments(invocation);
        return invocation.getMethod().invoke(self, argumentsForJava);
    }

    // invocation.getArguments() expands varargs, so we need to put them back into an array
    private Object[] unexpandArguments(InvocationOnMock invocation) {
        Method method = invocation.getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] unexpandedArguments = new Object[parameters.length];
        Object[] argumentsWithExpandedVarArgs = invocation.getArguments();
        for (int i = 0; i < unexpandedArguments.length; i++) {
            if (parameters[i].isVarArgs()) {
                // Wrap all remaining arguments into an array
                int varArgSize = argumentsWithExpandedVarArgs.length - parameters.length + 1;
                Object varArgs = Array.newInstance(parameters[i].getType().getComponentType(), varArgSize);
                if (varArgSize > 0) {
                    System.arraycopy(argumentsWithExpandedVarArgs, i, varArgs, 0, varArgSize);
                }
                unexpandedArguments[i] = varArgs;
            } else {
                unexpandedArguments[i] = argumentsWithExpandedVarArgs[i];
            }
        }
        return unexpandedArguments;
    }

    private static class StubDetectingInvocationListener implements InvocationListener {
        private boolean lastInvocationWasMocked = false;

        @Override
        public void reportInvocation(MethodInvocationReport methodInvocationReport) {
            lastInvocationWasMocked = methodInvocationReport.getLocationOfStubbing() != null;
        }
    }
}
