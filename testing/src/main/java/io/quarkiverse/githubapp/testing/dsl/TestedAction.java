package io.quarkiverse.githubapp.testing.dsl;

@FunctionalInterface
public interface TestedAction<T extends Throwable> {

    void run() throws T;

}
