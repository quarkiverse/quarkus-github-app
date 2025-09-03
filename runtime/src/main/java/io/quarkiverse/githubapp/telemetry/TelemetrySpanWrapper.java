package io.quarkiverse.githubapp.telemetry;

/**
 * A wrapper for the span.
 */
public interface TelemetrySpanWrapper {

    default <T extends TelemetrySpanWrapper> T as(Class<T> clazz) {
        return (T) this;
    }
}
