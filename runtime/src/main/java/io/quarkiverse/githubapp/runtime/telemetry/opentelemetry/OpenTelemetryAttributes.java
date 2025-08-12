package io.quarkiverse.githubapp.runtime.telemetry.opentelemetry;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.AttributesBuilder;
import io.quarkiverse.githubapp.GitHubEvent;

public class OpenTelemetryAttributes {

    public static final AttributeKey<Long> INSTALLATION_ID = AttributeKey.longKey("gitHubEvent.installationId");
    public static final AttributeKey<String> APP_NAME = AttributeKey.stringKey("gitHubEvent.appName");
    public static final AttributeKey<String> DELIVERY_ID = AttributeKey.stringKey("gitHubEvent.deliveryId");
    public static final AttributeKey<String> REPOSITORY = AttributeKey.stringKey("gitHubEvent.repository");
    public static final AttributeKey<String> EVENT = AttributeKey.stringKey("gitHubEvent.event");
    public static final AttributeKey<String> ACTION = AttributeKey.stringKey("gitHubEvent.action");
    public static final AttributeKey<String> EVENT_ACTION = AttributeKey.stringKey("gitHubEvent.eventAction");
    public static final AttributeKey<String> PAYLOAD = AttributeKey.stringKey("gitHubEvent.payload");
    public static final AttributeKey<String> CLASS = AttributeKey
            .stringKey("gitHubEvent.eventMethod.class");
    public static final AttributeKey<String> METHOD = AttributeKey
            .stringKey("gitHubEvent.eventMethod.method");
    public static final AttributeKey<String> METHOD_SIGNATURE = AttributeKey
            .stringKey("gitHubEvent.eventMethod.methodSignature");
    public static final AttributeKey<String> OUTCOME = AttributeKey.stringKey("outcome");
    public static final AttributeKey<String> ERROR_MESSAGE = AttributeKey.stringKey("errorMessage");

    public static final String STATUS_FAILURE = "failure";
    public static final String STATUS_SUCCESS = "success";

    static void putSpanGitHubEventAttributes(AttributesBuilder attributesBuilder, GitHubEvent gitHubEvent) {
        attributesBuilder.put(DELIVERY_ID, gitHubEvent.getDeliveryId());
        attributesBuilder.put(INSTALLATION_ID, gitHubEvent.getInstallationId());
        if (gitHubEvent.getAppName().isPresent()) {
            attributesBuilder.put(APP_NAME, gitHubEvent.getAppName().get());
        }
    }

    static void putCommonGitHubEventAttributes(AttributesBuilder attributesBuilder, GitHubEvent gitHubEvent) {
        if (gitHubEvent.getRepository().isPresent()) {
            attributesBuilder.put(REPOSITORY, gitHubEvent.getRepository().get());
        }
        attributesBuilder.put(EVENT, gitHubEvent.getEvent());
        attributesBuilder.put(EVENT_ACTION, gitHubEvent.getEventAction());
    }

    static void putCommonGitHubEventMethodAttributes(AttributesBuilder attributesBuilder, String className, String methodName,
            String signature) {
        attributesBuilder.put(OpenTelemetryAttributes.CLASS, className);
        attributesBuilder.put(OpenTelemetryAttributes.METHOD, methodName);
        attributesBuilder.put(OpenTelemetryAttributes.METHOD_SIGNATURE, signature);
    }

    static void putSuccessAttributes(AttributesBuilder attributesBuilder) {
        attributesBuilder.put(OUTCOME, STATUS_SUCCESS);
    }

    static void putErrorAttributes(AttributesBuilder attributesBuilder, Throwable throwable) {
        attributesBuilder.put(OUTCOME, STATUS_FAILURE);
        attributesBuilder.put(ERROR_MESSAGE, throwable.getMessage());
    }
}
