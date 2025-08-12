package io.quarkiverse.githubapp.telemetry;

public interface TelemetrySpanWrapper {

    default <T extends TelemetrySpanWrapper> T as(Class<T> clazz) {
        return (T) this;
    }
}
