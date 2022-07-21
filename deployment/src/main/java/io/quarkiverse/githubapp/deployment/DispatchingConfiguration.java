package io.quarkiverse.githubapp.deployment;

import static io.quarkiverse.githubapp.deployment.GitHubAppDotNames.DYNAMIC_GRAPHQL_CLIENT;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;

class DispatchingConfiguration {

    private final Map<String, EventDispatchingConfiguration> eventConfigurations = new TreeMap<>();

    private final Map<DotName, TreeSet<EventDispatchingMethod>> methods = new TreeMap<>();

    public Map<String, EventDispatchingConfiguration> getEventConfigurations() {
        return eventConfigurations;
    }

    public EventDispatchingConfiguration getOrCreateEventConfiguration(String event, String payloadType) {
        return eventConfigurations.computeIfAbsent(event, et -> new EventDispatchingConfiguration(event, payloadType));
    }

    public Map<DotName, TreeSet<EventDispatchingMethod>> getMethods() {
        return methods;
    }

    public void addEventDispatchingMethod(EventDispatchingMethod eventDispatchingMethod) {
        methods.computeIfAbsent(eventDispatchingMethod.getMethod().declaringClass().name(), k -> new TreeSet<>())
                .add(eventDispatchingMethod);
    }

    public boolean requiresGraphQLClient() {
        for (EventDispatchingMethod eventDispatchingMethod : methods.values().stream().flatMap(Set::stream)
                .collect(Collectors.toList())) {
            if (eventDispatchingMethod.requiresGraphQLClient()) {
                return true;
            }
        }

        return false;
    }

    static class EventDispatchingConfiguration {

        private final String event;

        private final String payloadType;

        private final TreeMap<String, EventAnnotation> eventAnnotations = new TreeMap<>();

        EventDispatchingConfiguration(String event, String payloadType) {
            this.event = event;
            this.payloadType = payloadType;
        }

        public String getEvent() {
            return event;
        }

        public String getPayloadType() {
            return payloadType;
        }

        public TreeMap<String, EventAnnotation> getEventAnnotations() {
            return eventAnnotations;
        }

        public Set<EventAnnotationLiteral> getEventAnnotationLiterals() {
            Set<EventAnnotationLiteral> literals = new HashSet<>();
            for (EventAnnotation eventAnnotation : eventAnnotations.values()) {
                literals.add(new EventAnnotationLiteral(eventAnnotation.getName(),
                        eventAnnotation.getValues().stream().map(av -> av.name()).collect(Collectors.toList())));
            }
            return literals;
        }

        public EventDispatchingConfiguration addEventAnnotation(String action, AnnotationInstance annotationInstance,
                List<AnnotationValue> values) {
            eventAnnotations.put(action, new EventAnnotation(annotationInstance.name(), values));
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
            return method.parameterTypes().stream().map(t -> t.name()).anyMatch(n -> DYNAMIC_GRAPHQL_CLIENT.equals(n));
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
