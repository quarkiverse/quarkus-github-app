package io.quarkiverse.githubapp.deployment;

import java.io.Reader;
import java.io.StringReader;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.inject.Singleton;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GitHub;

import io.quarkiverse.githubapp.event.Actions;
import io.quarkiverse.githubapp.runtime.GitHubEventDispatcher;
import io.quarkiverse.githubapp.runtime.Routes;
import io.quarkiverse.githubapp.runtime.github.GitHubService;
import io.quarkiverse.githubapp.runtime.signing.JwtTokenCreator;
import io.quarkiverse.githubapp.runtime.signing.PayloadSignatureChecker;
import io.quarkiverse.githubapp.runtime.smee.SmeeIoForwarder;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AutoAddScopeBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.MethodDescriptors;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;

class GithubAppProcessor {

    private static final String FEATURE = "github-app";

    private static final MethodDescriptor ARC_CONTAINER_INSTANCE_METHOD_DESCRIPTOR = MethodDescriptor
            .ofMethod(ArcContainer.class, "instance", InstanceHandle.class, Class.class, Annotation[].class);
    private static final MethodDescriptor INSTANCE_HANDLE_GET_METHOD_DESCRIPTOR = MethodDescriptor.ofMethod(InstanceHandle.class,
            "get", Object.class);

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem requireSsl() {
        return new ExtensionSslNativeSupportBuildItem(FEATURE);
    }

    @BuildStep
    void additionalBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        additionalBeans.produce(new AdditionalBeanBuildItem.Builder().addBeanClasses(JwtTokenCreator.class,
                PayloadSignatureChecker.class,
                GitHubService.class,
                SmeeIoForwarder.class,
                Routes.class)
                .setUnremovable()
                .setDefaultScope(io.quarkus.arc.processor.DotNames.SINGLETON)
                .build());
    }

    @BuildStep
    void generateDispatcher(CombinedIndexBuildItem combinedIndex,
            BuildProducer<AutoAddScopeBuildItem> autoAddScope,
            BuildProducer<GeneratedBeanBuildItem> generatedBean,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans) {
        Collection<EventDefinition> allEventDefinitions = getAllEventDefinitions(combinedIndex.getIndex());

        DotName[] subscriberAnnotations = allEventDefinitions.stream().map(d -> d.getAnnotation()).toArray(DotName[]::new);

        autoAddScope.produce(AutoAddScopeBuildItem.builder()
                .containsAnnotations(subscriberAnnotations)
                .defaultScope(io.quarkus.arc.processor.DotNames.SINGLETON)
                .build());

        for (DotName subscriberAnnotation : subscriberAnnotations) {
            unremovableBeans.produce(UnremovableBeanBuildItem.beanClassAnnotation(subscriberAnnotation));
        }

        Map<String, EventDispatchingConfiguration> eventDispatchingConfigurations = getEventDispatchingConfigurations(
                combinedIndex.getIndex(), allEventDefinitions);

        ClassOutput classOutput = new GeneratedBeanGizmoAdaptor(generatedBean);

        ClassCreator classCreator = ClassCreator.builder().classOutput(classOutput)
                .className(GitHubEventDispatcher.class.getName() + "Impl")
                .interfaces(GitHubEventDispatcher.class)
                .build();
        classCreator.addAnnotation(Singleton.class);

        MethodCreator dispatchMethodCreator = classCreator.getMethodCreator(
                "dispatch",
                void.class,
                GitHub.class, String.class, String.class, String.class);

        ResultHandle arcContainerRh = dispatchMethodCreator.invokeStaticMethod(MethodDescriptors.ARC_CONTAINER);

        // we will need to split this in different methods, probably one per type
        for (EventDispatchingConfiguration eventDispatchingConfiguration : eventDispatchingConfigurations.values()) {
            ResultHandle gitHubRh = dispatchMethodCreator.getMethodParam(0);
            ResultHandle dispatchedEventRh = dispatchMethodCreator.getMethodParam(1);
            ResultHandle dispatchedActionRh = dispatchMethodCreator.getMethodParam(2);
            ResultHandle dispatchedPayloadRh = dispatchMethodCreator.getMethodParam(3);

            ResultHandle eventRh = dispatchMethodCreator.load(eventDispatchingConfiguration.getEvent());
            String payloadType = eventDispatchingConfiguration.getPayload();

            BytecodeCreator eventMatchesCreator = dispatchMethodCreator
                    .ifTrue(dispatchMethodCreator.invokeVirtualMethod(MethodDescriptors.OBJECT_EQUALS, eventRh, dispatchedEventRh))
                    .trueBranch();

            ResultHandle payloadInstanceRh = eventMatchesCreator.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(GitHub.class, "parseEventPayload", GHEventPayload.class, Reader.class,
                            Class.class),
                    gitHubRh,
                    eventMatchesCreator.newInstance(MethodDescriptor.ofConstructor(StringReader.class, String.class),
                            dispatchedPayloadRh),
                    eventMatchesCreator.loadClass(payloadType));

            for (Entry<String, TreeSet<EventDispatchingMethod>> eventDispatchingMethodsEntry : eventDispatchingConfiguration.getMethods().entrySet()) {
                String action = eventDispatchingMethodsEntry.getKey();
                if (Actions.ALL.equals(action)) {
                    for (EventDispatchingMethod eventDispatchingMethod : eventDispatchingMethodsEntry.getValue()) {
                        eventMatchesCreator.invokeVirtualMethod(
                                MethodDescriptor.ofMethod(eventDispatchingMethod.getClassName(),
                                        eventDispatchingMethod.getMethodName(), void.class, payloadType),
                                eventMatchesCreator.invokeInterfaceMethod(INSTANCE_HANDLE_GET_METHOD_DESCRIPTOR,
                                        eventMatchesCreator.invokeInterfaceMethod(ARC_CONTAINER_INSTANCE_METHOD_DESCRIPTOR,
                                                arcContainerRh,
                                                eventMatchesCreator
                                                        .loadClass(eventDispatchingMethod.getClassName()),
                                                eventMatchesCreator.newArray(Annotation.class, 0))),
                                payloadInstanceRh);
                    }
                } else {
                    BytecodeCreator actionMatchesCreator = eventMatchesCreator
                            .ifTrue(eventMatchesCreator.invokeVirtualMethod(MethodDescriptors.OBJECT_EQUALS, eventMatchesCreator.load(action), dispatchedActionRh))
                            .trueBranch();

                    for (EventDispatchingMethod eventDispatchingMethod : eventDispatchingMethodsEntry.getValue()) {
                        actionMatchesCreator.invokeVirtualMethod(
                                MethodDescriptor.ofMethod(eventDispatchingMethod.getClassName(),
                                        eventDispatchingMethod.getMethodName(), void.class, payloadType),
                                actionMatchesCreator.invokeInterfaceMethod(INSTANCE_HANDLE_GET_METHOD_DESCRIPTOR,
                                        actionMatchesCreator.invokeInterfaceMethod(ARC_CONTAINER_INSTANCE_METHOD_DESCRIPTOR,
                                                arcContainerRh,
                                                actionMatchesCreator
                                                        .loadClass(eventDispatchingMethod.getClassName()),
                                                actionMatchesCreator.newArray(Annotation.class, 0))),
                                payloadInstanceRh);
                    }
                }
            }
        }
        dispatchMethodCreator.returnValue(null);

        classCreator.close();
    }

    private static Collection<EventDefinition> getAllEventDefinitions(IndexView index) {
        Collection<EventDefinition> mainEventDefinitions = new ArrayList<>();
        Collection<EventDefinition> allEventDefinitions = new ArrayList<>();

        for (AnnotationInstance eventInstance : index.getAnnotations(DotNames.EVENT)) {
            if (eventInstance.target().kind() == Kind.CLASS) {
                mainEventDefinitions.add(new EventDefinition(eventInstance.target().asClass().name(),
                        eventInstance.value("name").asString(),
                        null,
                        eventInstance.value("payload").asClass().name()));
            }
        }

        allEventDefinitions.addAll(mainEventDefinitions);

        for (EventDefinition mainEventDefinition : mainEventDefinitions) {
            for (AnnotationInstance eventInstance : index.getAnnotations(mainEventDefinition.getAnnotation())) {
                if (eventInstance.target().kind() == Kind.CLASS) {
                    AnnotationValue actionValue = eventInstance.value();

                    allEventDefinitions.add(new EventDefinition(eventInstance.target().asClass().name(),
                            mainEventDefinition.getEvent(),
                            actionValue != null ? actionValue.asString() : null,
                            mainEventDefinition.getPayload()));
                }
            }
        }

        return allEventDefinitions;
    }

    private static Map<String, EventDispatchingConfiguration> getEventDispatchingConfigurations(
            IndexView index, Collection<EventDefinition> allEventDefinitions) {
        Map<String, EventDispatchingConfiguration> eventDispatchingConfiguration = new TreeMap<>();

        for (EventDefinition eventDefinition : allEventDefinitions) {
            Collection<AnnotationInstance> eventSubscriberInstances = index.getAnnotations(eventDefinition.getAnnotation())
                    .stream()
                    .filter(ai -> ai.target().kind() == Kind.METHOD_PARAMETER)
                    .collect(Collectors.toList());
            for (AnnotationInstance eventSubscriberInstance : eventSubscriberInstances) {
                String action = eventDefinition.getAction() != null ? eventDefinition.getAction()
                        : (eventSubscriberInstance.value() != null ? eventSubscriberInstance.value().asString() : null);

                MethodInfo methodInfo = eventSubscriberInstance.target().asMethodParameter().method();
                List<Type> methodParameters = methodInfo.parameters();
                if (methodParameters.size() != 1 || !methodParameters.get(0).name().equals(eventDefinition.getPayload())) {
                    throw new IllegalStateException(
                            "Method subscribing to a GitHub '" + eventDefinition.getEvent()
                                    + "' event should have only one parameter of type '" + eventDefinition.getPayload()
                                    + "'. Offending method: " + methodInfo.declaringClass().name() + "#" + methodInfo);
                }

                eventDispatchingConfiguration
                        .computeIfAbsent(eventDefinition.getEvent(),
                                e -> new EventDispatchingConfiguration(e, eventDefinition.getPayload().toString()))
                        .addMethod(action, new EventDispatchingMethod(methodInfo.declaringClass().name().toString(), methodInfo.name()));
            }
        }

        return eventDispatchingConfiguration;
    }

    static class EventDispatchingConfiguration {

        private final String event;

        private final String payload;

        private final TreeMap<String, TreeSet<EventDispatchingMethod>> methods = new TreeMap<>();

        EventDispatchingConfiguration(String event, String payload) {
            this.event = event;
            this.payload = payload;
        }

        public String getEvent() {
            return event;
        }

        public String getPayload() {
            return payload;
        }

        public TreeMap<String, TreeSet<EventDispatchingMethod>> getMethods() {
            return methods;
        }

        void addMethod(String action, EventDispatchingMethod method) {
            methods.computeIfAbsent(action, k -> new TreeSet<>()).add(method);
        }
    }

    static class EventDispatchingMethod implements Comparable<EventDispatchingMethod> {

        private final String className;

        private final String methodName;

        EventDispatchingMethod(String className, String methodName) {
            this.className = className;
            this.methodName = methodName;
        }

        public String getClassName() {
            return className;
        }

        public String getMethodName() {
            return methodName;
        }

        @Override
        public int compareTo(EventDispatchingMethod other) {
            int classNameCompareTo = className.compareTo(other.className);
            if (classNameCompareTo != 0) {
                return classNameCompareTo;
            }

            return methodName.compareTo(other.methodName);
        }
    }

    static class EventDefinition {

        private final DotName annotation;

        private final String event;

        private final String action;

        private final DotName payload;

        EventDefinition(DotName annotation, String event, String action, DotName payload) {
            this.annotation = annotation;
            this.event = event;
            this.action = action;
            this.payload = payload;
        }

        DotName getAnnotation() {
            return annotation;
        }

        String getEvent() {
            return event;
        }

        String getAction() {
            return action;
        }

        public DotName getPayload() {
            return payload;
        }

        boolean isWildCard() {
            return action == null || Actions.ALL.equals(action);
        }
    }
}
