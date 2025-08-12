package io.quarkiverse.githubapp.it.app.opentelemetry;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricExporter;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;
import io.quarkiverse.githubapp.runtime.telemetry.opentelemetry.OpenTelemetryAttributes;
import io.quarkiverse.githubapp.runtime.telemetry.opentelemetry.OpenTelemetryNames;
import io.quarkus.vertx.web.Route;

public class OpenTelemetryExporterRoute {

    @Inject
    InMemorySpanExporter inMemorySpanExporter;

    @Inject
    InMemoryMetricExporter inMemoryMetricExporter;

    @Inject
    OpenTelemetry openTelemetry;

    @Route(path = "/opentelemetry/exporter/spans", methods = Route.HttpMethod.GET)
    public String exportTelemetrySpans() {
        if (openTelemetry instanceof OpenTelemetrySdk openTelemetrySdk) {
            openTelemetrySdk.getSdkLoggerProvider().forceFlush().join(1_000, MILLISECONDS);
        }

        waitForTracesToArrive(3);

        List<SpanData> spans = inMemorySpanExporter.getFinishedSpanItems();

        // issues.opened
        Optional<SpanData> gitHubEventSpanOpenedOptional = spans.stream()
                .filter(s -> s.getName().equals(OpenTelemetryNames.GITHUB_EVENT + " issues.opened")).findAny();
        Optional<SpanData> gitHubEventMethodSpanOpenedOptional = spans.stream().filter(s -> s.getName().startsWith(
                OpenTelemetryNames.GITHUB_EVENT_METHOD + " " + GitHubEventListener.class.getName() + ".issueOpened"))
                .findAny();

        Assertions.assertThat(gitHubEventSpanOpenedOptional).isPresent();
        Assertions.assertThat(gitHubEventMethodSpanOpenedOptional).isPresent();

        SpanData gitHubEventOpenedSpan = gitHubEventSpanOpenedOptional.get();
        assertThat(gitHubEventOpenedSpan).hasStatus(StatusData.ok());
        assertCommonSpanAttributes(gitHubEventOpenedSpan.getAttributes(), "issues.opened");
        String gitHubEventOpenedSpanId = gitHubEventOpenedSpan.getSpanId();

        SpanData gitHubEventMethodOpenedSpan = gitHubEventMethodSpanOpenedOptional.get();
        assertThat(gitHubEventMethodOpenedSpan).hasStatus(StatusData.ok());
        assertThat(gitHubEventMethodOpenedSpan.getName()).isEqualTo(
                OpenTelemetryNames.GITHUB_EVENT_METHOD
                        + " io.quarkiverse.githubapp.it.app.opentelemetry.GitHubEventListener.issueOpened");
        assertCommonSpanAttributes(gitHubEventMethodOpenedSpan.getAttributes(), "issues.opened");
        assertEventMethodAttributes(gitHubEventMethodOpenedSpan.getAttributes(), "issueOpened");
        assertThat(gitHubEventMethodOpenedSpan).hasLinksSatisfying(
                link -> assertThat(link.get(0).getSpanContext().getSpanId()).isEqualTo(gitHubEventOpenedSpanId));

        // issues.closed
        Optional<SpanData> gitHubEventSpanClosedOptional = spans.stream()
                .filter(s -> s.getName().equals(OpenTelemetryNames.GITHUB_EVENT + " issues.closed")).findAny();
        Optional<SpanData> gitHubEventMethodSpanClosedOptional = spans.stream().filter(s -> s.getName().startsWith(
                OpenTelemetryNames.GITHUB_EVENT_METHOD + " " + GitHubEventListener.class.getName() + ".issueClosed"))
                .findAny();

        Assertions.assertThat(gitHubEventSpanClosedOptional).isPresent();
        Assertions.assertThat(gitHubEventMethodSpanClosedOptional).isPresent();

        // assert github-event issues.closed span
        SpanData gitHubEventClosedSpan = gitHubEventSpanClosedOptional.get();
        assertThat(gitHubEventClosedSpan).hasStatus(StatusData.ok());
        assertCommonSpanAttributes(gitHubEventClosedSpan.getAttributes(), "issues.closed");
        String gitHubEventClosedSpanId = gitHubEventClosedSpan.getSpanId();

        // assert github-event-method for issues.closed span
        SpanData gitHubEventMethodClosedSpan = gitHubEventMethodSpanClosedOptional.get();
        assertThat(gitHubEventMethodClosedSpan)
                .hasStatus(StatusData.create(StatusCode.ERROR, "An expected error to test error path"));
        assertThat(gitHubEventMethodClosedSpan.getName()).isEqualTo(
                OpenTelemetryNames.GITHUB_EVENT_METHOD
                        + " io.quarkiverse.githubapp.it.app.opentelemetry.GitHubEventListener.issueClosed");
        assertCommonSpanAttributes(gitHubEventMethodClosedSpan.getAttributes(), "issues.closed");
        assertEventMethodAttributes(gitHubEventMethodClosedSpan.getAttributes(), "issueClosed");
        assertThat(gitHubEventMethodClosedSpan).hasLinksSatisfying(
                link -> assertThat(link.get(0).getSpanContext().getSpanId()).isEqualTo(gitHubEventClosedSpanId));

        return "OK";
    }

    @Route(path = "/opentelemetry/exporter/metrics", methods = Route.HttpMethod.GET)
    public String exportTelemetryMetrics() {
        if (openTelemetry instanceof OpenTelemetrySdk openTelemetrySdk) {
            openTelemetrySdk.getSdkLoggerProvider().forceFlush().join(1_000, MILLISECONDS);
        }

        waitForMetricsToArrive(1);

        List<MetricData> metrics = inMemoryMetricExporter.getFinishedMetricItems()
                .stream()
                .filter(md -> md.getName().startsWith("github-app.")).toList();

        // issues.opened
        LongPointData gitHubEventMetricOpenedPointData = findLongPointData(metrics, OpenTelemetryNames.GITHUB_EVENT,
                "issues.opened")
                .orElseThrow(() -> new IllegalStateException(
                        "Unable to find metric: " + OpenTelemetryNames.GITHUB_EVENT + " for event issues.opened"));
        assertMetricSuccess(gitHubEventMetricOpenedPointData.getAttributes());
        assertCommonMetricAttributes(gitHubEventMetricOpenedPointData.getAttributes(), "issues.opened");

        LongPointData gitHubEventMethodMetricOpenedPointData = findLongPointData(metrics,
                OpenTelemetryNames.GITHUB_EVENT_METHOD,
                "issues.opened")
                .orElseThrow(
                        () -> new IllegalStateException("Unable to find metric: " + OpenTelemetryNames.GITHUB_EVENT_METHOD
                                + " for event issues.opened"));
        assertMetricSuccess(gitHubEventMethodMetricOpenedPointData.getAttributes());
        assertCommonMetricAttributes(gitHubEventMethodMetricOpenedPointData.getAttributes(), "issues.opened");
        assertEventMethodAttributes(gitHubEventMethodMetricOpenedPointData.getAttributes(), "issueOpened");

        // issues.closed
        LongPointData gitHubEventMetricClosedPointData = findLongPointData(metrics, OpenTelemetryNames.GITHUB_EVENT,
                "issues.closed")
                .orElseThrow(() -> new IllegalStateException(
                        "Unable to find metric: " + OpenTelemetryNames.GITHUB_EVENT + " for event issues.closed"));
        assertMetricSuccess(gitHubEventMetricClosedPointData.getAttributes());
        assertCommonMetricAttributes(gitHubEventMetricClosedPointData.getAttributes(), "issues.closed");

        LongPointData gitHubEventMethodMetricClosedPointData = findLongPointData(metrics,
                OpenTelemetryNames.GITHUB_EVENT_METHOD,
                "issues.closed")
                .orElseThrow(
                        () -> new IllegalStateException("Unable to find metric: " + OpenTelemetryNames.GITHUB_EVENT_METHOD
                                + " for event issues.closed"));
        assertMetricFailure(gitHubEventMethodMetricClosedPointData.getAttributes(), "An expected error to test error path");
        assertCommonMetricAttributes(gitHubEventMethodMetricClosedPointData.getAttributes(), "issues.closed");
        assertEventMethodAttributes(gitHubEventMethodMetricClosedPointData.getAttributes(), "issueClosed");

        return "OK";
    }

    private static Optional<LongPointData> findLongPointData(Collection<MetricData> metrics, String name, String eventAction) {
        for (MetricData metricData : metrics) {
            if (!name.equals(metricData.getName())) {
                continue;
            }

            for (LongPointData pointData : metricData.getLongSumData().getPoints()) {
                if (!eventAction.equals(pointData.getAttributes().get(OpenTelemetryAttributes.EVENT_ACTION))) {
                    continue;
                }
                return Optional.of(pointData);
            }
        }
        return Optional.empty();
    }

    private static void assertCommonSpanAttributes(Attributes attributes, String eventAction) {
        assertThat(attributes).containsEntry(OpenTelemetryAttributes.INSTALLATION_ID, 13173124L);
        assertThat(attributes).containsEntry(OpenTelemetryAttributes.REPOSITORY,
                "yrodiere/quarkus-bot-java-playground");
        assertThat(attributes).containsKey(OpenTelemetryAttributes.DELIVERY_ID);
        assertThat(attributes).containsEntry(OpenTelemetryAttributes.EVENT, "issues");
        assertThat(attributes).containsEntry(OpenTelemetryAttributes.EVENT_ACTION, eventAction);
    }

    private static void assertEventMethodAttributes(Attributes attributes, String methodName) {
        assertThat(attributes).containsEntry(OpenTelemetryAttributes.CLASS,
                GitHubEventListener.class.getName());
        assertThat(attributes).containsEntry(OpenTelemetryAttributes.METHOD, methodName);
        assertThat(attributes).containsEntry(OpenTelemetryAttributes.METHOD_SIGNATURE,
                "void " + methodName
                        + "(io.quarkiverse.githubapp.GitHubEvent gitHubEvent, org.kohsuke.github.GitHub gitHub) throws java.io.IOException");
    }

    private static void assertCommonMetricAttributes(Attributes attributes, String eventAction) {
        assertThat(attributes).containsEntry(OpenTelemetryAttributes.REPOSITORY,
                "yrodiere/quarkus-bot-java-playground");
        assertThat(attributes).containsEntry(OpenTelemetryAttributes.EVENT, "issues");
        assertThat(attributes).containsEntry(OpenTelemetryAttributes.EVENT_ACTION, eventAction);
    }

    private static void assertMetricSuccess(Attributes attributes) {
        assertThat(attributes).containsEntry(OpenTelemetryAttributes.OUTCOME,
                OpenTelemetryAttributes.STATUS_SUCCESS);
    }

    private static void assertMetricFailure(Attributes attributes, String errorMessage) {
        assertThat(attributes).containsEntry(OpenTelemetryAttributes.OUTCOME,
                OpenTelemetryAttributes.STATUS_FAILURE);
        assertThat(attributes).containsEntry(OpenTelemetryAttributes.ERROR_MESSAGE,
                errorMessage);
    }

    private void waitForTracesToArrive(int expectedTracesCount) {
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    assertThat(inMemorySpanExporter.getFinishedSpanItems().size()).isGreaterThanOrEqualTo(expectedTracesCount);
                });
    }

    private void waitForMetricsToArrive(int expectedMetricsCount) {
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    assertThat(inMemoryMetricExporter.getFinishedMetricItems().size())
                            .isGreaterThanOrEqualTo(expectedMetricsCount);
                });
    }
}
