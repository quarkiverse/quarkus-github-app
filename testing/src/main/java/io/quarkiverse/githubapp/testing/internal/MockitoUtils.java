package io.quarkiverse.githubapp.testing.internal;

import java.util.function.Supplier;

public class MockitoUtils {

    private MockitoUtils() {
    }

    public static <T> T doWithMockedClassClassLoader(Class<?> mockedClass, Supplier<T> action) {
        ClassLoader mockedClassClassLoader = mockedClass.getClassLoader();
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(mockedClassClassLoader);
            return action.get();
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
    }

}
