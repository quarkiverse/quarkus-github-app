package io.quarkiverse.githubapp.deployment;

import java.io.Reader;
import java.io.StringReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.enterprise.event.Event;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;
import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GitHub;

import io.quarkiverse.githubapp.deployment.DispatchingConfiguration.EventAnnotation;
import io.quarkiverse.githubapp.deployment.DispatchingConfiguration.EventAnnotationLiteral;
import io.quarkiverse.githubapp.deployment.DispatchingConfiguration.EventDispatchingConfiguration;
import io.quarkiverse.githubapp.deployment.DispatchingConfiguration.EventDispatchingMethod;
import io.quarkiverse.githubapp.event.Actions;
import io.quarkiverse.githubapp.runtime.GitHubEventDispatcher;
import io.quarkiverse.githubapp.runtime.Routes;
import io.quarkiverse.githubapp.runtime.github.GitHubService;
import io.quarkiverse.githubapp.runtime.signing.JwtTokenCreator;
import io.quarkiverse.githubapp.runtime.signing.PayloadSignatureChecker;
import io.quarkiverse.githubapp.runtime.smee.SmeeIoForwarder;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AutoAddScopeBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.arc.processor.MethodDescriptors;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.gizmo.AnnotatedElement;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.runtime.util.HashUtil;

class GithubAppProcessor {

    private static final Logger LOG = Logger.getLogger(GithubAppProcessor.class);

    private static final String FEATURE = "github-app";

    private static final MethodDescriptor EVENT_SELECT = MethodDescriptor.ofMethod(Event.class, "select", Event.class,
            Annotation[].class);
    private static final MethodDescriptor EVENT_FIRE = MethodDescriptor.ofMethod(Event.class, "fire", void.class, Object.class);

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
                .setDefaultScope(DotNames.SINGLETON)
                .build());
    }

    @BuildStep
    void generateDispatcher(CombinedIndexBuildItem combinedIndex,
            BuildProducer<AutoAddScopeBuildItem> autoAddScope,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<GeneratedBeanBuildItem> generatedBeans,
            BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans) {
        Collection<EventDefinition> allEventDefinitions = getAllEventDefinitions(combinedIndex.getIndex());

        // Add the qualifiers as beans
        String[] subscriberAnnotations = allEventDefinitions.stream().map(d -> d.getAnnotation().toString())
                .toArray(String[]::new);
        additionalBeans.produce(new AdditionalBeanBuildItem(subscriberAnnotations));

        DispatchingConfiguration dispatchingConfiguration = getDispatchingConfiguration(
                combinedIndex.getIndex(), allEventDefinitions);

        ClassOutput classOutput = new GeneratedClassGizmoAdaptor(generatedClasses, true);
        generateAnnotationLiterals(classOutput, dispatchingConfiguration);

        ClassOutput beanClassOutput = new GeneratedBeanGizmoAdaptor(generatedBeans);
        generateDispatcher(beanClassOutput, dispatchingConfiguration);
        generateMultiplexers(beanClassOutput, dispatchingConfiguration);
    }

    private static Collection<EventDefinition> getAllEventDefinitions(IndexView index) {
        Collection<EventDefinition> mainEventDefinitions = new ArrayList<>();
        Collection<EventDefinition> allEventDefinitions = new ArrayList<>();

        for (AnnotationInstance eventInstance : index.getAnnotations(GitHubAppDotNames.EVENT)) {
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
                            mainEventDefinition.getPayloadType()));
                }
            }
        }

        return allEventDefinitions;
    }

    private static DispatchingConfiguration getDispatchingConfiguration(
            IndexView index, Collection<EventDefinition> allEventDefinitions) {
        DispatchingConfiguration configuration = new DispatchingConfiguration();

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
                if (methodParameters.size() != 1 || !methodParameters.get(0).name().equals(eventDefinition.getPayloadType())) {
                    throw new IllegalStateException(
                            "Method subscribing to a GitHub '" + eventDefinition.getEvent()
                                    + "' event should have only one parameter of type '" + eventDefinition.getPayloadType()
                                    + "'. Offending method: " + methodInfo.declaringClass().name() + "#" + methodInfo);
                }

                configuration
                        .getOrCreateEventConfiguration(eventDefinition.getEvent(), eventDefinition.getPayloadType().toString())
                        .addEventAnnotation(action, eventSubscriberInstance);
                configuration.addEventDispatchingMethod(new EventDispatchingMethod(eventSubscriberInstance, methodInfo));
            }
        }

        return configuration;
    }

    private static void generateAnnotationLiterals(ClassOutput classOutput, DispatchingConfiguration dispatchingConfiguration) {
        for (EventDispatchingConfiguration eventDispatchingConfiguration : dispatchingConfiguration.getEventConfigurations()
                .values()) {
            for (EventAnnotationLiteral eventAnnotationLiteral : eventDispatchingConfiguration.getEventAnnotationLiterals()) {
                String literalClassName = getLiteralClassName(eventAnnotationLiteral.getName());

                String signature = String.format("Ljavax/enterprise/util/AnnotationLiteral<L%1$s;>;L%1$s;",
                        eventAnnotationLiteral.getName().toString().replace('.', '/'));

                ClassCreator literalClassCreator = ClassCreator.builder().classOutput(classOutput)
                        .className(literalClassName)
                        .signature(signature)
                        .superClass(AnnotationLiteral.class)
                        .interfaces(eventAnnotationLiteral.getName().toString())
                        .build();

                Class<?>[] parameterTypes = new Class<?>[eventAnnotationLiteral.getAttributes().size()];
                Arrays.fill(parameterTypes, String.class);

                MethodCreator constructorCreator = literalClassCreator.getMethodCreator("<init>", "V",
                        (Object[]) parameterTypes);
                constructorCreator.invokeSpecialMethod(MethodDescriptor.ofConstructor(AnnotationLiteral.class),
                        constructorCreator.getThis());
                for (int i = 0; i < eventAnnotationLiteral.getAttributes().size(); i++) {
                    constructorCreator.writeInstanceField(
                            FieldDescriptor.of(literalClassName, eventAnnotationLiteral.getAttributes().get(i), String.class),
                            constructorCreator.getThis(), constructorCreator.getMethodParam(i));
                    constructorCreator.setModifiers(Modifier.PUBLIC);
                }
                constructorCreator.returnValue(null);

                for (String attribute : eventAnnotationLiteral.getAttributes()) {
                    // we only support String for now
                    literalClassCreator.getFieldCreator(attribute, String.class)
                            .setModifiers(Modifier.PRIVATE);
                    MethodCreator getterCreator = literalClassCreator.getMethodCreator(attribute, String.class);
                    getterCreator.setModifiers(Modifier.PUBLIC);
                    getterCreator.returnValue(getterCreator.readInstanceField(
                            FieldDescriptor.of(literalClassName, attribute, String.class), getterCreator.getThis()));
                }

                literalClassCreator.close();
            }
        }
    }

    private static void generateDispatcher(ClassOutput beanClassOutput, DispatchingConfiguration dispatchingConfiguration) {
        ClassCreator dispatcherClassCreator = ClassCreator.builder().classOutput(beanClassOutput)
                .className(GitHubEventDispatcher.class.getName() + "Impl")
                .interfaces(GitHubEventDispatcher.class)
                .build();

        dispatcherClassCreator.addAnnotation(Singleton.class);

        FieldCreator eventFieldCreator = dispatcherClassCreator.getFieldCreator("event", Event.class);
        eventFieldCreator.addAnnotation(Inject.class);
        eventFieldCreator.setModifiers(Modifier.PROTECTED);

        MethodCreator dispatchMethodCreator = dispatcherClassCreator.getMethodCreator(
                "dispatch",
                void.class,
                GitHub.class, String.class, String.class, String.class);
        dispatchMethodCreator.setModifiers(Modifier.PUBLIC);

        ResultHandle gitHubRh = dispatchMethodCreator.getMethodParam(0);
        ResultHandle dispatchedEventRh = dispatchMethodCreator.getMethodParam(1);
        ResultHandle dispatchedActionRh = dispatchMethodCreator.getMethodParam(2);
        ResultHandle dispatchedPayloadRh = dispatchMethodCreator.getMethodParam(3);

        for (EventDispatchingConfiguration eventDispatchingConfiguration : dispatchingConfiguration.getEventConfigurations()
                .values()) {
            ResultHandle eventRh = dispatchMethodCreator.load(eventDispatchingConfiguration.getEvent());
            String payloadType = eventDispatchingConfiguration.getPayloadType();

            BytecodeCreator eventMatchesCreator = dispatchMethodCreator
                    .ifTrue(dispatchMethodCreator.invokeVirtualMethod(MethodDescriptors.OBJECT_EQUALS, eventRh,
                            dispatchedEventRh))
                    .trueBranch();

            ResultHandle payloadInstanceRh = eventMatchesCreator.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(GitHub.class, "parseEventPayload", GHEventPayload.class, Reader.class,
                            Class.class),
                    gitHubRh,
                    eventMatchesCreator.newInstance(MethodDescriptor.ofConstructor(StringReader.class, String.class),
                            dispatchedPayloadRh),
                    eventMatchesCreator.loadClass(payloadType));

            for (Entry<String, EventAnnotation> eventAnnotationEntry : eventDispatchingConfiguration.getEventAnnotations()
                    .entrySet()) {
                String action = eventAnnotationEntry.getKey();
                EventAnnotation eventAnnotation = eventAnnotationEntry.getValue();

                Class<?>[] literalParameterTypes = new Class<?>[eventAnnotation.getValues().size()];
                Arrays.fill(literalParameterTypes, String.class);
                List<ResultHandle> literalParameters = new ArrayList<>();

                ResultHandle annotationLiteral = eventMatchesCreator.newInstance(MethodDescriptor
                        .ofConstructor(getLiteralClassName(eventAnnotation.getName()), (Object[]) literalParameterTypes),
                        literalParameters.toArray(ResultHandle[]::new));
                ResultHandle annotationLiteralArray = eventMatchesCreator.newArray(Annotation.class, 1);
                eventMatchesCreator.writeArrayValue(annotationLiteralArray, 0, annotationLiteral);

                if (Actions.ALL.equals(action)) {
                    ResultHandle cdiEventRh = eventMatchesCreator.invokeInterfaceMethod(EVENT_SELECT,
                            eventMatchesCreator.readInstanceField(
                                    FieldDescriptor.of(dispatcherClassCreator.getClassName(), "event", Event.class),
                                    eventMatchesCreator.getThis()),
                            annotationLiteralArray);
                    eventMatchesCreator.invokeInterfaceMethod(EVENT_FIRE, cdiEventRh, payloadInstanceRh);
                } else {
                    BytecodeCreator actionMatchesCreator = eventMatchesCreator
                            .ifTrue(eventMatchesCreator.invokeVirtualMethod(MethodDescriptors.OBJECT_EQUALS,
                                    eventMatchesCreator.load(action), dispatchedActionRh))
                            .trueBranch();

                    ResultHandle cdiEventRh = actionMatchesCreator.invokeInterfaceMethod(EVENT_SELECT,
                            actionMatchesCreator.readInstanceField(
                                    FieldDescriptor.of(dispatcherClassCreator.getClassName(), "event", Event.class),
                                    eventMatchesCreator.getThis()),
                            annotationLiteralArray);
                    actionMatchesCreator.invokeInterfaceMethod(EVENT_FIRE, cdiEventRh, payloadInstanceRh);
                }
            }
        }

        dispatchMethodCreator.returnValue(null);

        dispatcherClassCreator.close();
    }

    private static void generateMultiplexers(ClassOutput beanClassOutput, DispatchingConfiguration dispatchingConfiguration) {
        for (Entry<DotName, TreeSet<EventDispatchingMethod>> eventDispatchingMethodsEntry : dispatchingConfiguration
                .getMethods().entrySet()) {
            DotName declaringClassName = eventDispatchingMethodsEntry.getKey();
            TreeSet<EventDispatchingMethod> eventDispatchingMethods = eventDispatchingMethodsEntry.getValue();
            ClassInfo declaringClass = eventDispatchingMethods.iterator().next().getMethod().declaringClass();

            if (BuiltinScope.isDeclaredOn(declaringClass)) {
                LOG.warn("Classes listening to GitHub events may not be annotated with CDI scopes annotations.");
            }

            ClassCreator multiplexerClassCreator = ClassCreator.builder().classOutput(beanClassOutput)
                    .className(declaringClassName + "_Mutiplexer")
                    .superClass(declaringClassName.toString())
                    .build();

            multiplexerClassCreator.addAnnotation(Singleton.class);

            for (AnnotationInstance classAnnotation : declaringClass.classAnnotations()) {
                multiplexerClassCreator.addAnnotation(classAnnotation);
            }

            // TODO we should "copy" the constructor too as it could be used for injection and initialization

            for (EventDispatchingMethod eventDispatchingMethod : eventDispatchingMethods) {
                AnnotationInstance eventSubscriberInstance = eventDispatchingMethod.getEventSubscriberInstance();
                MethodInfo method = eventDispatchingMethod.getMethod();
                Map<Short, List<AnnotationInstance>> parameterAnnotationMapping = method.annotations().stream()
                        .filter(ai -> ai.target().kind() == Kind.METHOD_PARAMETER)
                        .collect(Collectors.groupingBy(ai -> ai.target().asMethodParameter().position()));

                // if the method already has an @Observes annotation
                if (method.hasAnnotation(DotNames.OBSERVES)) {
                    LOG.warn("Methods listening to GitHub events may not be annotated with @Observes. Offending method: "
                            + method.declaringClass().name() + "#" + method);
                }

                MethodCreator methodCreator = multiplexerClassCreator.getMethodCreator(
                        method.name() + "_" + HashUtil.sha1(eventSubscriberInstance.toString()),
                        method.returnType().name().toString(),
                        method.parameters().stream().map(p -> p.name().toString()).toArray(String[]::new));

                ResultHandle[] parameterValues = new ResultHandle[method.parameters().size()];

                for (short i = 0; i < method.parameters().size(); i++) {
                    List<AnnotationInstance> parameterAnnotations = parameterAnnotationMapping.get(i);
                    AnnotatedElement generatedParameterAnnotations = methodCreator.getParameterAnnotations(i);
                    if (parameterAnnotations.stream().anyMatch(ai -> ai.name().equals(eventSubscriberInstance.name()))) {
                        generatedParameterAnnotations.addAnnotation(DotNames.OBSERVES.toString());
                        generatedParameterAnnotations.addAnnotation(eventSubscriberInstance);
                    } else {
                        for (AnnotationInstance annotationInstance : parameterAnnotations) {
                            generatedParameterAnnotations.addAnnotation(annotationInstance);
                        }
                    }

                    parameterValues[i] = methodCreator.getMethodParam(i);
                }

                ResultHandle returnValue = methodCreator.invokeVirtualMethod(method, methodCreator.getThis(), parameterValues);
                methodCreator.returnValue(returnValue);
            }

            multiplexerClassCreator.close();
        }
    }

    private static String getLiteralClassName(DotName annotationName) {
        return annotationName + "_AnnotationLiteral";
    }
}
