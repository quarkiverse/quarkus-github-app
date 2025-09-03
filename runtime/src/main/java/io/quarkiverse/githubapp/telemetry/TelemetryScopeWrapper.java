package io.quarkiverse.githubapp.telemetry;

/**
 * A wrapper for the scope.
 */
public interface TelemetryScopeWrapper extends AutoCloseable {

    default <T extends TelemetryScopeWrapper> T as(Class<T> clazz) {
        return (T) this;
    }
}
