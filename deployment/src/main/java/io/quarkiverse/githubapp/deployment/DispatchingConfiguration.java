package io.quarkiverse.githubapp.deployment;

import static io.quarkiverse.githubapp.deployment.GitHubAppDotNames.DYNAMIC_GRAPHQL_CLIENT;
import static io.quarkiverse.githubapp.deployment.GitHubAppDotNames.GITHUB_EVENT;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

class DispatchingConfiguration {

    /**
     * The key is the name of the event (e.g. pull_request)
     */
    private final Map<String, EventDispatchingConfiguration> eventConfigurations = new TreeMap<>();

    /**
     * The key is the name of the declaring class.
     */
    private final Map<DotName, TreeSet<EventDispatchingMethod>> methods = new TreeMap<>();

    public Map<String, EventDispatchingConfiguration> getEventConfigurations() {
        return eventConfigurations;
    }

    public EventDispatchingConfiguration getOrCreateEventConfiguration(String event, String payloadType) {
        EventDispatchingConfiguration eventDispatchingConfiguration = eventConfigurations.computeIfAbsent(event,
                et -> new EventDispatchingConfiguration(event));

        // we are also collecting raw events here and their payload type is null
        if (eventDispatchingConfiguration.getPayloadType() == null
                && payloadType != null && !GITHUB_EVENT.toString().equals(payloadType)) {
            eventDispatchingConfiguration.setPayloadType(payloadType);
        }

        return eventDispatchingConfiguration;
    }

    public Map<DotName, TreeSet<EventDispatchingMethod>> getMethods() {
        return methods;
    }

    public void addEventDispatchingMethod(EventDispatchingMethod eventDispatchingMethod) {
        methods.computeIfAbsent(eventDispatchingMethod.getMethod().declaringClass().name(), k -> new TreeSet<>())
                .add(eventDispatchingMethod);
    }

    public boolean requiresGraphQLClient() {
        for (EventDispatchingMethod eventDispatchingMethod : methods.values().stream().flatMap(Set::stream).toList()) {
            if (eventDispatchingMethod.requiresGraphQLClient()) {
                return true;
            }
        }

        return false;
    }

    static class EventDispatchingConfiguration {

        private final String event;

        private String payloadType;

        /**
         * The key is the name of the action.
         * <p>
         * There can be more than one EventAnnotation for a given action as we might have a {@code @RawEvent} annotation.
         */
        private final TreeMap<String, Set<EventAnnotation>> eventAnnotations = new TreeMap<>();

        EventDispatchingConfiguration(String event) {
            this.event = event;
        }

        public String getEvent() {
            return event;
        }

        public void setPayloadType(String payloadType) {
            this.payloadType = payloadType;
        }

        public String getPayloadType() {
            return payloadType;
        }

        public TreeMap<String, Set<EventAnnotation>> getEventAnnotations() {
            return eventAnnotations;
        }

        public Set<EventAnnotationLiteral> getEventAnnotationLiterals() {
            Set<EventAnnotationLiteral> literals = new HashSet<>();
            for (Set<EventAnnotation> eventAnnotations : eventAnnotations.values()) {
                for (EventAnnotation eventAnnotation : eventAnnotations) {
                    literals.add(new EventAnnotationLiteral(eventAnnotation.getName(),
                            eventAnnotation.getValues().stream().map(AnnotationValue::name).toList()));
                }
            }
            return literals;
        }

        public EventDispatchingConfiguration addEventAnnotation(String action, AnnotationInstance annotationInstance,
                List<AnnotationValue> values) {
            eventAnnotations.computeIfAbsent(action, a -> new TreeSet<>())
                    .add(new EventAnnotation(annotationInstance.name(), values));
            return this;
        }
    }

    static class EventAnnotation implements Comparable<EventAnnotation> {

        private final DotName name;

        private final List<AnnotationValue> values;

        EventAnnotation(DotName name, List<AnnotationValue> values) {
            this.name = name;
            this.values = values;
        }

        public DotName getName() {
            return name;
        }

        public boolean isRawEvent() {
            return GitHubAppDotNames.RAW_EVENT.equals(name);
        }

        public List<AnnotationValue> getValues() {
            return values;
        }

        @Override
        public int compareTo(EventAnnotation other) {
            int nameCompareTo = name.compareTo(other.name);
            if (nameCompareTo != 0) {
                return nameCompareTo;
            }
            int valuesLengthCompare = Integer.compare(values.size(), other.values.size());
            if (valuesLengthCompare != 0) {
                return valuesLengthCompare;
            }
            for (int i = 0; i < values.size(); i++) {
                // we only support string for now, we can adjust later
                int valueCompare = values.get(i).asString().compareTo(other.values.get(i).asString());
                if (valueCompare != 0) {
                    return valueCompare;
                }
            }

            return 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, values);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            EventAnnotation other = (EventAnnotation) o;

            return Objects.equals(this.name, other.name) && Objects.equals(this.values, other.values);
        }
    }

    static class EventAnnotationLiteral {

        private final DotName name;

        // for now, we only support string attributes
        private final List<String> attributes;

        EventAnnotationLiteral(DotName name, List<String> attributes) {
            this.name = name;
            this.attributes = attributes;
        }

        public DotName getName() {
            return name;
        }

        public List<String> getAttributes() {
            return attributes;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            EventAnnotationLiteral other = (EventAnnotationLiteral) obj;

            return Objects.equals(name, other.name) &&
                    Objects.equals(attributes, other.attributes);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, attributes);
        }
    }

    static class EventDispatchingMethod implements Comparable<EventDispatchingMethod> {

        private final AnnotationInstance eventSubscriberInstance;

        private final MethodInfo method;

        EventDispatchingMethod(AnnotationInstance eventSubscriberInstance, MethodInfo method) {
            this.eventSubscriberInstance = eventSubscriberInstance;
            this.method = method;
        }

        public AnnotationInstance getEventSubscriberInstance() {
            return eventSubscriberInstance;
        }

        public MethodInfo getMethod() {
            return method;
        }

        public boolean requiresGraphQLClient() {
            return method.parameterTypes().stream().map(Type::name).anyMatch(DYNAMIC_GRAPHQL_CLIENT::equals);
        }

        @Override
        public int compareTo(EventDispatchingMethod other) {
            int classNameCompareTo = method.declaringClass().name().compareTo(other.method.declaringClass().name());
            if (classNameCompareTo != 0) {
                return classNameCompareTo;
            }

            int methodNameComparator = method.toString().compareTo(other.method.toString());
            if (methodNameComparator != 0) {
                return methodNameComparator;
            }

            return eventSubscriberInstance.toString(false).compareTo(other.eventSubscriberInstance.toString(false));
        }
    }
}
