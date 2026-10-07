package io.quarkiverse.githubapp.runtime.telemetry.opentelemetry;

import java.net.http.HttpClient;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Reserve;
import jakarta.inject.Inject;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.javahttpclient.JavaHttpClientTelemetry;
import io.quarkiverse.githubapp.runtime.github.AbstractJavaHttpClientFactory;

@Dependent
@Reserve
@Priority(0)
public class OpenTelemetryJavaHttpClientFactory extends AbstractJavaHttpClientFactory {

    @Inject
    OpenTelemetry openTelemetry;

    @Override
    public HttpClient create() {
        return JavaHttpClientTelemetry.builder(openTelemetry).build().wrap(createDefaultClientBuilder().build());
    }
}
