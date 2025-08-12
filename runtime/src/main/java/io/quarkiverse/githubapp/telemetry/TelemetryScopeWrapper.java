package io.quarkiverse.githubapp.telemetry;

public interface TelemetryScopeWrapper extends AutoCloseable {

    default <T extends TelemetryScopeWrapper> T as(Class<T> clazz) {
        return (T) this;
    }
}
