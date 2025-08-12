package io.quarkiverse.githubapp.it.app.opentelemetry;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.opentelemetry.sdk.testing.exporter.InMemoryMetricExporter;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;

@ApplicationScoped
public class InMemoryExportersProducer {

    @Produces
    @Singleton
    InMemorySpanExporter inMemorySpanExporter() {
        return InMemorySpanExporter.create();
    }

    @Produces
    @Singleton
    InMemoryMetricExporter inMemoryMetricExporter() {
        return InMemoryMetricExporter.create();
    }

    @Produces
    @Singleton
    InMemoryMetricReader inMemoryMetricReader() {
        return InMemoryMetricReader.create();
    }
}
