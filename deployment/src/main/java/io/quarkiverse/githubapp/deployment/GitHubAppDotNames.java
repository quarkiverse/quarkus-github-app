package io.quarkiverse.githubapp.deployment;

import org.jboss.jandex.DotName;
import org.kohsuke.github.GitHub;

import io.quarkiverse.githubapp.ConfigFile;
import io.quarkiverse.githubapp.GitHubEvent;
import io.quarkiverse.githubapp.error.ErrorHandler;
import io.quarkiverse.githubapp.event.Event;
import io.quarkiverse.githubapp.event.RawEvent;
import io.quarkiverse.githubapp.runtime.Multiplexer;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClient;

final class GitHubAppDotNames {

    static final DotName EVENT = DotName.createSimple(Event.class.getName());
    static final DotName RAW_EVENT = DotName.createSimple(RawEvent.class.getName());
    static final DotName CONFIG_FILE = DotName.createSimple(ConfigFile.class.getName());
    static final DotName MULTIPLEXER = DotName.createSimple(Multiplexer.class.getName());
    static final DotName ERROR_HANDLER = DotName.createSimple(ErrorHandler.class.getName());

    static final DotName GITHUB = DotName.createSimple(GitHub.class.getName());
    static final DotName GITHUB_EVENT = DotName.createSimple(GitHubEvent.class.getName());
    static final DotName DYNAMIC_GRAPHQL_CLIENT = DotName.createSimple(DynamicGraphQLClient.class.getName());

    static final DotName OPENTELEMETRY_TRACES_REPORTER = DotName
            .createSimple("io.quarkiverse.githubapp.runtime.telemetry.opentelemetry.OpenTelemetryTracesReporter");
    static final DotName OPENTELEMETRY_METRICS_REPORTER = DotName
            .createSimple("io.quarkiverse.githubapp.runtime.telemetry.opentelemetry.OpenTelemetryMetricsReporter");
    static final DotName OPENTELEMETRY_JAVA_HTTP_CLIENT_FACTORY = DotName
            .createSimple("io.quarkiverse.githubapp.runtime.telemetry.opentelemetry.OpenTelemetryJavaHttpClientFactory");
    static final DotName JAVA_HTTP_CLIENT_TELEMETRY = DotName
            .createSimple("io.opentelemetry.instrumentation.httpclient.JavaHttpClientTelemetry");

    private GitHubAppDotNames() {
    }
}
