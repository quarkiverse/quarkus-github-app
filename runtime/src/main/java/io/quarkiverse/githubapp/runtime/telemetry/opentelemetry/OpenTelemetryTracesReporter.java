package io.quarkiverse.githubapp.runtime.telemetry.opentelemetry;

import jakarta.inject.Singleton;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.quarkiverse.githubapp.GitHubEvent;
import io.quarkiverse.githubapp.runtime.config.CheckedConfigProvider;
import io.quarkiverse.githubapp.runtime.telemetry.noop.NoopTelemetryTracesReporter;
import io.quarkiverse.githubapp.telemetry.CommandErrorType;
import io.quarkiverse.githubapp.telemetry.TelemetryScopeWrapper;
import io.quarkiverse.githubapp.telemetry.TelemetrySpanWrapper;
import io.quarkiverse.githubapp.telemetry.TelemetryTracesReporter;
import io.quarkus.arc.DefaultBean;

@Singleton
@DefaultBean
public class OpenTelemetryTracesReporter implements TelemetryTracesReporter {

    private static final NoopTelemetryTracesReporter NOOP_INSTANCE = new NoopTelemetryTracesReporter();

    private final Tracer tracer;

    private final boolean tracesActive;
    private final boolean recordEventPayload;
    private final boolean recordCommand;

    OpenTelemetryTracesReporter(Tracer tracer, CheckedConfigProvider checkedConfigProvider) {
        this.tracer = tracer;
        this.tracesActive = checkedConfigProvider.telemetry().tracesActive();
        this.recordEventPayload = checkedConfigProvider.telemetry().recordEventPayload();
        this.recordCommand = checkedConfigProvider.telemetry().recordCommand();
    }

    @Override
    public void reportEarlyRequestError(String deliveryId, String event, String error) {
        if (!tracesActive) {
            return;
        }

        Span requestSpan = Span.current();
        if (!requestSpan.getSpanContext().isValid()) {
            return;
        }

        requestSpan.setAttribute(OpenTelemetryAttributes.DELIVERY_ID, deliveryId);
        requestSpan.setAttribute(OpenTelemetryAttributes.EVENT, event);
        requestSpan.setStatus(StatusCode.ERROR, error);
    }

    @Override
    public TelemetrySpanWrapper createGitHubEventSpan(GitHubEvent gitHubEvent) {
        if (!tracesActive) {
            return NOOP_INSTANCE.createGitHubEventSpan(gitHubEvent);
        }

        SpanBuilder spanBuilder = tracer
                .spanBuilder(OpenTelemetryNames.GITHUB_EVENT.concat(" ").concat(gitHubEvent.getEventAction()));
        spanBuilder.setSpanKind(SpanKind.INTERNAL);
        AttributesBuilder attributesBuilder = Attributes.builder();
        OpenTelemetryAttributes.putSpanGitHubEventAttributes(attributesBuilder, gitHubEvent);
        OpenTelemetryAttributes.putCommonGitHubEventAttributes(attributesBuilder, gitHubEvent);
        if (recordEventPayload) {
            attributesBuilder.put(OpenTelemetryAttributes.PAYLOAD, gitHubEvent.getPayload());
        }
        spanBuilder.setAllAttributes(attributesBuilder.build());

        return new OpenTelemetrySpanWrapper(spanBuilder.startSpan());
    }

    @Override
    public GitHubEvent decorateGitHubEvent(GitHubEvent originalGitHubEvent, TelemetrySpanWrapper spanWrapper) {
        if (!tracesActive) {
            return originalGitHubEvent;
        }

        return new OpenTelemetryDecoratedGitHubEvent(originalGitHubEvent,
                spanWrapper.as(OpenTelemetrySpanWrapper.class).span().getSpanContext());
    }

    @Override
    public TelemetrySpanWrapper createGitHubEventListeningMethodSpan(GitHubEvent gitHubEvent, String className,
            String methodName, String signature) {
        if (!tracesActive) {
            return NOOP_INSTANCE.createGitHubEventListeningMethodSpan(gitHubEvent, className, methodName, signature);
        }

        StringBuilder spanNameSb = new StringBuilder(className.length() + methodName.length() + 32);
        spanNameSb.append(OpenTelemetryNames.GITHUB_EVENT_METHOD).append(" ").append(className).append(".")
                .append(methodName);

        SpanBuilder eventMethodSpanBuilder = tracer.spanBuilder(spanNameSb.toString());
        eventMethodSpanBuilder.setSpanKind(SpanKind.INTERNAL);

        AttributesBuilder attributesBuilder = Attributes.builder();
        OpenTelemetryAttributes.putSpanGitHubEventAttributes(attributesBuilder, gitHubEvent);
        OpenTelemetryAttributes.putCommonGitHubEventAttributes(attributesBuilder, gitHubEvent);
        OpenTelemetryAttributes.putCommonGitHubEventMethodAttributes(attributesBuilder, className, methodName, signature);
        eventMethodSpanBuilder.setAllAttributes(attributesBuilder.build());

        eventMethodSpanBuilder.addLink(gitHubEvent.as(OpenTelemetryDecoratedGitHubEvent.class).getRootSpanContext());

        return new OpenTelemetrySpanWrapper(eventMethodSpanBuilder.startSpan());
    }

    @Override
    public void reportSuccess(GitHubEvent gitHubEvent, TelemetrySpanWrapper spanWrapper) {
        if (!tracesActive) {
            return;
        }

        Span span = spanWrapper.as(OpenTelemetrySpanWrapper.class).span();
        span.setStatus(StatusCode.OK);
    }

    @Override
    public void reportException(GitHubEvent gitHubEvent, TelemetrySpanWrapper spanWrapper, Throwable e) {
        if (!tracesActive) {
            return;
        }

        Span span = spanWrapper.as(OpenTelemetrySpanWrapper.class).span();
        span.recordException(e);
        span.setStatus(StatusCode.ERROR, e.getMessage());
    }

    @Override
    public TelemetryScopeWrapper makeCurrent(TelemetrySpanWrapper spanWrapper) {
        if (!tracesActive) {
            return NOOP_INSTANCE.makeCurrent(spanWrapper);
        }

        return new OpenTelemetryScopeWrapper(spanWrapper.as(OpenTelemetrySpanWrapper.class).span().makeCurrent());
    }

    @Override
    public void endSpan(TelemetrySpanWrapper spanWrapper) {
        if (!tracesActive) {
            return;
        }

        spanWrapper.as(OpenTelemetrySpanWrapper.class).span().end();
    }

    @Override
    public void reportCommandMethodSuccess(GitHubEvent gitHubEvent, String commandClassName, String commandLine) {
        if (!tracesActive) {
            return;
        }
        Span span = Span.current();
        if (span == null) {
            return;
        }

        StringBuilder eventNameSb = new StringBuilder(commandClassName.length() + 26);
        eventNameSb.append(OpenTelemetryNames.COMMAND_METHOD).append(" ").append(commandClassName);

        AttributesBuilder attributesBuilder = Attributes.builder();
        OpenTelemetryAttributes.putCommonCommandAttributes(attributesBuilder, commandClassName);
        OpenTelemetryAttributes.putCommandSuccessAttributes(attributesBuilder);
        if (recordCommand && commandLine != null) {
            attributesBuilder.put(OpenTelemetryAttributes.COMMAND_LINE, commandLine);
        }

        span.addEvent(eventNameSb.toString(), attributesBuilder.build());
    }

    @Override
    public void reportCommandMethodError(GitHubEvent gitHubEvent, String commandClassName,
            String commandLine, CommandErrorType errorType, String errorMessage) {
        if (!tracesActive) {
            return;
        }
        Span span = Span.current();
        if (span == null) {
            return;
        }

        String eventName = OpenTelemetryNames.COMMAND_METHOD;
        // when we can't parse the command, we might end up not having a command
        if (commandClassName != null) {
            eventName += " " + commandClassName;
        }

        AttributesBuilder attributesBuilder = Attributes.builder();
        OpenTelemetryAttributes.putCommonCommandAttributes(attributesBuilder, commandClassName);
        OpenTelemetryAttributes.putCommandErrorAttributes(attributesBuilder, errorType, errorMessage);
        if (recordCommand && commandLine != null) {
            attributesBuilder.put(OpenTelemetryAttributes.COMMAND_LINE, commandLine);
        }

        span.addEvent(eventName, attributesBuilder.build());
    }

    private record OpenTelemetrySpanWrapper(Span span) implements TelemetrySpanWrapper {
    }

    private record OpenTelemetryScopeWrapper(Scope scope) implements TelemetryScopeWrapper {

        @Override
        public void close() throws Exception {
            scope.close();
        }
    }
}
