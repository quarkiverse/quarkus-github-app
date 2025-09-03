package io.quarkiverse.githubapp.runtime.telemetry.opentelemetry;

import java.net.http.HttpClient;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.httpclient.JavaHttpClientTelemetry;
import io.quarkiverse.githubapp.runtime.github.AbstractJavaHttpClientFactory;
import io.quarkus.arc.DefaultBean;

@Dependent
@DefaultBean
public class OpenTelemetryJavaHttpClientFactory extends AbstractJavaHttpClientFactory {

    @Inject
    OpenTelemetry openTelemetry;

    @Override
    public HttpClient create() {
        return JavaHttpClientTelemetry.builder(openTelemetry).build().newHttpClient(createDefaultClientBuilder().build());
    }
}
