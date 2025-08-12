package io.quarkiverse.githubapp.runtime.telemetry.opentelemetry;

import jakarta.inject.Singleton;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.quarkiverse.githubapp.GitHubEvent;
import io.quarkiverse.githubapp.runtime.config.CheckedConfigProvider;
import io.quarkiverse.githubapp.telemetry.TelemetryMetricsReporter;
import io.quarkus.arc.DefaultBean;

@Singleton
@DefaultBean
public class OpenTelemetryMetricsReporter implements TelemetryMetricsReporter {

    private final LongCounter gitHubEventCounter;

    private final LongCounter gitHubEventMethodCounter;

    private final boolean metricsActive;

    OpenTelemetryMetricsReporter(Meter meter, CheckedConfigProvider checkedConfigProvider) {
        metricsActive = checkedConfigProvider.telemetry().metricsActive();

        if (metricsActive) {
            gitHubEventCounter = meter.counterBuilder(OpenTelemetryNames.GITHUB_EVENT)
                    .setDescription("Counts total GitHub events handled")
                    .setUnit("event")
                    .build();
            gitHubEventMethodCounter = meter.counterBuilder(OpenTelemetryNames.GITHUB_EVENT_METHOD)
                    .setDescription("Counts total invocations of GitHub event methods")
                    .setUnit("invocation")
                    .build();
        } else {
            this.gitHubEventCounter = null;
            this.gitHubEventMethodCounter = null;
        }
    }

    @Override
    public void incrementGitHubEventSuccess(GitHubEvent gitHubEvent) {
        if (!metricsActive) {
            return;
        }

        AttributesBuilder attributesBuilder = Attributes.builder();
        OpenTelemetryAttributes.putSuccessAttributes(attributesBuilder);
        OpenTelemetryAttributes.putCommonGitHubEventAttributes(attributesBuilder, gitHubEvent);

        gitHubEventCounter.add(1L, attributesBuilder.build());
    }

    @Override
    public void incrementGitHubEventError(GitHubEvent gitHubEvent, Throwable throwable) {
        if (!metricsActive) {
            return;
        }

        AttributesBuilder attributesBuilder = Attributes.builder();
        OpenTelemetryAttributes.putErrorAttributes(attributesBuilder, throwable);
        OpenTelemetryAttributes.putCommonGitHubEventAttributes(attributesBuilder, gitHubEvent);

        gitHubEventCounter.add(1L, attributesBuilder.build());
    }

    @Override
    public void incrementGitHubEventMethodSuccess(GitHubEvent gitHubEvent, String className, String methodName,
            String signature) {
        if (!metricsActive) {
            return;
        }

        AttributesBuilder attributesBuilder = Attributes.builder();
        OpenTelemetryAttributes.putSuccessAttributes(attributesBuilder);
        OpenTelemetryAttributes.putCommonGitHubEventAttributes(attributesBuilder, gitHubEvent);
        OpenTelemetryAttributes.putCommonGitHubEventMethodAttributes(attributesBuilder, className, methodName, signature);

        gitHubEventMethodCounter.add(1L, attributesBuilder.build());
    }

    @Override
    public void incrementGitHubEventMethodError(GitHubEvent gitHubEvent, String className, String methodName, String signature,
            Throwable throwable) {
        if (!metricsActive) {
            return;
        }

        AttributesBuilder attributesBuilder = Attributes.builder();
        OpenTelemetryAttributes.putErrorAttributes(attributesBuilder, throwable);
        OpenTelemetryAttributes.putCommonGitHubEventAttributes(attributesBuilder, gitHubEvent);
        OpenTelemetryAttributes.putCommonGitHubEventMethodAttributes(attributesBuilder, className, methodName, signature);

        gitHubEventMethodCounter.add(1L, attributesBuilder.build());
    }

}
