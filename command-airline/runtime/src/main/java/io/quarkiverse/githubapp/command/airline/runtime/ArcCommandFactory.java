package io.quarkiverse.githubapp.command.airline.runtime;

import com.github.rvesse.airline.CommandFactory;

import io.quarkus.arc.Arc;

public class ArcCommandFactory<T> implements CommandFactory<T> {

    @Override
    @SuppressWarnings("unchecked")
    public T createInstance(Class<?> type) {
        T instance = (T) Arc.container().instance(type).get();

        if (instance == null) {
            throw new IllegalStateException("ArC was unable to instantiate command class " + type);
        }

        return instance;
    }
}
