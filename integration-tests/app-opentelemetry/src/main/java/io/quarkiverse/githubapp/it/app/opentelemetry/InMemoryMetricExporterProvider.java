package io.quarkiverse.githubapp.it.app.opentelemetry;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.metrics.ConfigurableMetricExporterProvider;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricExporter;
import io.quarkus.arc.Arc;

public class InMemoryMetricExporterProvider implements ConfigurableMetricExporterProvider {

    @Override
    public MetricExporter createExporter(ConfigProperties configProperties) {
        return Arc.container().instance(InMemoryMetricExporter.class).get();
    }

    @Override
    public String getName() {
        return "in-memory";
    }
}
