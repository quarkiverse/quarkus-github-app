package io.quarkiverse.githubapp.deployment;

import static io.quarkiverse.githubapp.deployment.GitHubAppDotNames.CONFIG_FILE;
import static io.quarkiverse.githubapp.deployment.GitHubAppDotNames.EVENT;
import static io.quarkiverse.githubapp.deployment.GitHubAppDotNames.GH_ROOT_OBJECTS;
import static io.quarkiverse.githubapp.deployment.GitHubAppDotNames.GH_SIMPLE_OBJECTS;

import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
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
import org.jboss.jandex.MethodParameterInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;
import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import io.jsonwebtoken.impl.DefaultJwtBuilder;
import io.jsonwebtoken.io.Deserializer;
import io.jsonwebtoken.io.Serializer;
import io.jsonwebtoken.jackson.io.JacksonDeserializer;
import io.jsonwebtoken.jackson.io.JacksonSerializer;
import io.quarkiverse.githubapp.GitHubEvent;
import io.quarkiverse.githubapp.deployment.DispatchingConfiguration.EventAnnotation;
import io.quarkiverse.githubapp.deployment.DispatchingConfiguration.EventAnnotationLiteral;
import io.quarkiverse.githubapp.deployment.DispatchingConfiguration.EventDispatchingConfiguration;
import io.quarkiverse.githubapp.deployment.DispatchingConfiguration.EventDispatchingMethod;
import io.quarkiverse.githubapp.event.Actions;
import io.quarkiverse.githubapp.runtime.ConfigFileReader;
import io.quarkiverse.githubapp.runtime.Routes;
import io.quarkiverse.githubapp.runtime.UtilsProducer;
import io.quarkiverse.githubapp.runtime.error.DefaultErrorHandler;
import io.quarkiverse.githubapp.runtime.error.ErrorHandlerBridgeFunction;
import io.quarkiverse.githubapp.runtime.github.GitHubService;
import io.quarkiverse.githubapp.runtime.github.PayloadHelper;
import io.quarkiverse.githubapp.runtime.signing.JwtTokenCreator;
import io.quarkiverse.githubapp.runtime.signing.PayloadSignatureChecker;
import io.quarkiverse.githubapp.runtime.smee.SmeeIoForwarder;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
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
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.gizmo.AnnotatedElement;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.CatchBlockCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TryBlock;
import io.quarkus.runtime.util.HashUtil;

class GithubAppProcessor {

    private static final Logger LOG = Logger.getLogger(GithubAppProcessor.class);

    private static final String FEATURE = "github-app";

    private static final String EVENT_EMITTER_FIELD = "eventEmitter";
    private static final String GITHUB_SERVICE_FIELD = "gitHubService";

    private static final MethodDescriptor EVENT_SELECT = MethodDescriptor.ofMethod(Event.class, "select", Event.class,
            Annotation[].class);
    private static final MethodDescriptor EVENT_FIRE_ASYNC = MethodDescriptor.ofMethod(Event.class, "fireAsync",
            CompletionStage.class, Object.class);
    private static final MethodDescriptor COMPLETION_STAGE_EXCEPTIONALLY = MethodDescriptor.ofMethod(CompletionStage.class,
            "exceptionally", CompletionStage.class, Function.class);

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem requireSsl() {
        return new ExtensionSslNativeSupportBuildItem(FEATURE);
    }

    @BuildStep
    IndexDependencyBuildItem indexGitHubApiJar() {
        return new IndexDependencyBuildItem("org.kohsuke", "github-api");
    }

    @BuildStep
    void registerForReflection(CombinedIndexBuildItem combinedIndex,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchies) {
        // Types used for config files
        for (AnnotationInstance configFileAnnotationInstance : combinedIndex.getIndex().getAnnotations(CONFIG_FILE)) {
            MethodParameterInfo methodParameter = configFileAnnotationInstance.target().asMethodParameter();
            short parameterPosition = methodParameter.position();
            Type parameterType = methodParameter.method().parameters().get(parameterPosition);
            reflectiveHierarchies.produce(new ReflectiveHierarchyBuildItem.Builder()
                    .type(parameterType)
                    .index(combinedIndex.getIndex())
                    .source(GithubAppProcessor.class.getSimpleName() + " > " + methodParameter.method().declaringClass() + "#"
                            + methodParameter.method())
                    .build());
        }

        // GitHub API
        for (DotName rootModelObject : GH_ROOT_OBJECTS) {
            reflectiveClasses.produce(new ReflectiveClassBuildItem(true, true, rootModelObject.toString()));

            reflectiveClasses.produce(new ReflectiveClassBuildItem(true, true,
                    combinedIndex.getIndex().getAllKnownSubclasses(rootModelObject).stream()
                            .map(ci -> ci.name().toString())
                            .toArray(String[]::new)));
        }

        reflectiveClasses.produce(new ReflectiveClassBuildItem(true, true,
                GH_SIMPLE_OBJECTS.stream().map(DotName::toString).toArray(String[]::new)));

        // Caffeine
        reflectiveClasses.produce(new ReflectiveClassBuildItem(true, true,
                "com.github.benmanes.caffeine.cache.SSMSA",
                "com.github.benmanes.caffeine.cache.PSWMS"));

        // JWT
        reflectiveClasses.produce(new ReflectiveClassBuildItem(true, true, DefaultJwtBuilder.class));
    }

    @BuildStep
    void jwtServiceProviderBuildItem(BuildProducer<ServiceProviderBuildItem> serviceProviders) {
        serviceProviders.produce(new ServiceProviderBuildItem(Serializer.class.getName(), JacksonSerializer.class.getName()));
        serviceProviders.produce(new ServiceProviderBuildItem(Deserializer.class.getName(), JacksonDeserializer.class.getName()));
    }

    @BuildStep
    void additionalBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        additionalBeans.produce(new AdditionalBeanBuildItem.Builder().addBeanClasses(JwtTokenCreator.class,
                PayloadSignatureChecker.class,
                GitHubService.class,
                SmeeIoForwarder.class,
                Routes.class,
                UtilsProducer.class,
                DefaultErrorHandler.class)
                .setUnremovable()
                .setDefaultScope(DotNames.SINGLETON)
                .build());
    }

    @BuildStep
    void generateClasses(CombinedIndexBuildItem combinedIndex,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<GeneratedBeanBuildItem> generatedBeans,
            BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
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
        generateDispatcher(beanClassOutput, combinedIndex, dispatchingConfiguration, reflectiveClasses);
        generateMultiplexers(beanClassOutput, dispatchingConfiguration, reflectiveClasses);
    }

    private static Collection<EventDefinition> getAllEventDefinitions(IndexView index) {
        Collection<EventDefinition> mainEventDefinitions = new ArrayList<>();
        Collection<EventDefinition> allEventDefinitions = new ArrayList<>();

        for (AnnotationInstance eventInstance : index.getAnnotations(EVENT)) {
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
                        : (eventSubscriberInstance.value() != null ? eventSubscriberInstance.value().asString() : Actions.ALL);

                MethodParameterInfo annotatedParameter = eventSubscriberInstance.target().asMethodParameter();
                MethodInfo methodInfo = annotatedParameter.method();
                DotName annotatedParameterType = annotatedParameter.method().parameters().get(annotatedParameter.position()).name();
                if (!eventDefinition.getPayloadType().equals(annotatedParameterType)) {
                    throw new IllegalStateException(
                            "Parameter subscribing to a GitHub '" + eventDefinition.getEvent()
                                    + "' event should be of type '" + eventDefinition.getPayloadType()
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

    private static void generateDispatcher(ClassOutput beanClassOutput,
            CombinedIndexBuildItem combinedIndex,
            DispatchingConfiguration dispatchingConfiguration,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        String dispatcherClassName = GitHubEvent.class.getName() + "DispatcherImpl";

        reflectiveClasses.produce(new ReflectiveClassBuildItem(true, true, dispatcherClassName));

        ClassCreator dispatcherClassCreator = ClassCreator.builder().classOutput(beanClassOutput)
                .className(dispatcherClassName)
                .build();

        dispatcherClassCreator.addAnnotation(Singleton.class);

        FieldCreator eventFieldCreator = dispatcherClassCreator.getFieldCreator(EVENT_EMITTER_FIELD, Event.class);
        eventFieldCreator.addAnnotation(Inject.class);
        eventFieldCreator.setModifiers(Modifier.PROTECTED);

        FieldCreator gitHubServiceFieldCreator = dispatcherClassCreator.getFieldCreator(GITHUB_SERVICE_FIELD, GitHubService.class);
        gitHubServiceFieldCreator.addAnnotation(Inject.class);
        gitHubServiceFieldCreator.setModifiers(Modifier.PROTECTED);

        MethodCreator dispatchMethodCreator = dispatcherClassCreator.getMethodCreator(
                "dispatch",
                void.class,
                GitHubEvent.class);
        dispatchMethodCreator.setModifiers(Modifier.PUBLIC);
        dispatchMethodCreator.getParameterAnnotations(0).addAnnotation(DotNames.OBSERVES.toString());

        ResultHandle gitHubEventRh = dispatchMethodCreator.getMethodParam(0);

        ResultHandle installationIdRh = dispatchMethodCreator.invokeVirtualMethod(
                MethodDescriptor.ofMethod(GitHubEvent.class, "getInstallationId", Long.class),
                gitHubEventRh);
        ResultHandle dispatchedEventRh = dispatchMethodCreator.invokeVirtualMethod(
                MethodDescriptor.ofMethod(GitHubEvent.class, "getEvent", String.class),
                gitHubEventRh);
        ResultHandle dispatchedActionRh = dispatchMethodCreator.invokeVirtualMethod(
                MethodDescriptor.ofMethod(GitHubEvent.class, "getAction", String.class),
                gitHubEventRh);
        ResultHandle dispatchedPayloadRh = dispatchMethodCreator.invokeVirtualMethod(
                MethodDescriptor.ofMethod(GitHubEvent.class, "getPayload", String.class),
                gitHubEventRh);

        TryBlock tryBlock = dispatchMethodCreator.tryBlock();

        ResultHandle gitHubRh = tryBlock.invokeVirtualMethod(
                MethodDescriptor.ofMethod(GitHubService.class, "getInstallationClient", GitHub.class, Long.class),
                tryBlock.readInstanceField(
                        FieldDescriptor.of(dispatcherClassCreator.getClassName(), GITHUB_SERVICE_FIELD, GitHubService.class),
                        tryBlock.getThis()),
                installationIdRh);

        for (EventDispatchingConfiguration eventDispatchingConfiguration : dispatchingConfiguration.getEventConfigurations()
                .values()) {
            ResultHandle eventRh = tryBlock.load(eventDispatchingConfiguration.getEvent());
            String payloadType = eventDispatchingConfiguration.getPayloadType();

            BytecodeCreator eventMatchesCreator = tryBlock
                    .ifTrue(tryBlock.invokeVirtualMethod(MethodDescriptors.OBJECT_EQUALS, eventRh,
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

                ResultHandle annotationLiteralRh = eventMatchesCreator.newInstance(MethodDescriptor
                        .ofConstructor(getLiteralClassName(eventAnnotation.getName()), (Object[]) literalParameterTypes),
                        literalParameters.toArray(ResultHandle[]::new));
                ResultHandle annotationLiteralArrayRh = eventMatchesCreator.newArray(Annotation.class, 1);
                eventMatchesCreator.writeArrayValue(annotationLiteralArrayRh, 0, annotationLiteralRh);

                if (Actions.ALL.equals(action)) {
                    fireAsyncAction(eventMatchesCreator, dispatcherClassCreator.getClassName(), gitHubEventRh, payloadInstanceRh,
                            annotationLiteralArrayRh);
                } else {
                    BytecodeCreator actionMatchesCreator = eventMatchesCreator
                            .ifTrue(eventMatchesCreator.invokeVirtualMethod(MethodDescriptors.OBJECT_EQUALS,
                                    eventMatchesCreator.load(action), dispatchedActionRh))
                            .trueBranch();

                    fireAsyncAction(actionMatchesCreator, dispatcherClassCreator.getClassName(), gitHubEventRh, payloadInstanceRh,
                            annotationLiteralArrayRh);
                }
            }
        }

        CatchBlockCreator catchBlockCreator = tryBlock.addCatch(Throwable.class);
        catchBlockCreator.invokeVirtualMethod(MethodDescriptor.ofMethod(ErrorHandlerBridgeFunction.class, "apply", Void.class, Throwable.class),
                catchBlockCreator.newInstance(MethodDescriptor.ofConstructor(ErrorHandlerBridgeFunction.class, GitHubEvent.class), gitHubEventRh),
                catchBlockCreator.getCaughtException());

        dispatchMethodCreator.returnValue(null);

        dispatcherClassCreator.close();
    }

    private static ResultHandle fireAsyncAction(BytecodeCreator bytecodeCreator, String className, ResultHandle gitHubEventRh,
            ResultHandle payloadInstanceRh, ResultHandle annotationLiteralArrayRh) {
        ResultHandle cdiEventRh = bytecodeCreator.invokeInterfaceMethod(EVENT_SELECT,
                bytecodeCreator.readInstanceField(
                        FieldDescriptor.of(className, EVENT_EMITTER_FIELD, Event.class),
                        bytecodeCreator.getThis()),
                annotationLiteralArrayRh);

        ResultHandle fireAsyncCompletionStageRH = bytecodeCreator.invokeInterfaceMethod(EVENT_FIRE_ASYNC, cdiEventRh,
                payloadInstanceRh);

        return bytecodeCreator.invokeInterfaceMethod(COMPLETION_STAGE_EXCEPTIONALLY, fireAsyncCompletionStageRH,
                bytecodeCreator.newInstance(
                        MethodDescriptor.ofConstructor(ErrorHandlerBridgeFunction.class, GitHubEvent.class,
                                GHEventPayload.class),
                        gitHubEventRh, payloadInstanceRh));
    }

    private static void generateMultiplexers(ClassOutput beanClassOutput,
            DispatchingConfiguration dispatchingConfiguration,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        for (Entry<DotName, TreeSet<EventDispatchingMethod>> eventDispatchingMethodsEntry : dispatchingConfiguration
                .getMethods().entrySet()) {
            DotName declaringClassName = eventDispatchingMethodsEntry.getKey();
            TreeSet<EventDispatchingMethod> eventDispatchingMethods = eventDispatchingMethodsEntry.getValue();
            ClassInfo declaringClass = eventDispatchingMethods.iterator().next().getMethod().declaringClass();

            reflectiveClasses.produce(new ReflectiveClassBuildItem(true, true, declaringClassName.toString()));

            if (BuiltinScope.isDeclaredOn(declaringClass)) {
                LOG.warn(
                        "Classes listening to GitHub events may not be annotated with CDI scopes annotations. Offending class: "
                                + declaringClass.name());
            }

            String multiplexerClassName = declaringClassName + "_Multiplexer";
            reflectiveClasses.produce(new ReflectiveClassBuildItem(true, true, multiplexerClassName));

            ClassCreator multiplexerClassCreator = ClassCreator.builder().classOutput(beanClassOutput)
                    .className(multiplexerClassName)
                    .superClass(declaringClassName.toString())
                    .build();

            multiplexerClassCreator.addAnnotation(Singleton.class);

            for (AnnotationInstance classAnnotation : declaringClass.classAnnotations()) {
                multiplexerClassCreator.addAnnotation(classAnnotation);
            }

            for (MethodInfo originalConstructor : declaringClass.constructors()) {
                MethodCreator constructorCreator = multiplexerClassCreator.getMethodCreator(MethodDescriptor.ofConstructor(multiplexerClassName,
                        originalConstructor.parameters().stream().map(t -> t.name().toString()).toArray(String[]::new)));

                List<AnnotationInstance> originalMethodAnnotations = originalConstructor.annotations().stream()
                        .filter(ai -> ai.target().kind() == Kind.METHOD).collect(Collectors.toList());
                for (AnnotationInstance originalMethodAnnotation : originalMethodAnnotations) {
                    constructorCreator.addAnnotation(originalMethodAnnotation);
                }

                Map<Short, List<AnnotationInstance>> originalConstructorParameterAnnotationMapping = originalConstructor.annotations().stream()
                        .filter(ai -> ai.target().kind() == Kind.METHOD_PARAMETER)
                        .collect(Collectors.groupingBy(ai -> ai.target().asMethodParameter().position()));

                List<ResultHandle> parametersRh = new ArrayList<>();
                for (short i = 0; i < originalConstructor.parameters().size(); i++) {
                    parametersRh.add(constructorCreator.getMethodParam(i));

                    AnnotatedElement parameterAnnotations = constructorCreator.getParameterAnnotations(i);
                    List<AnnotationInstance> originalConstructorParameterAnnotations = originalConstructorParameterAnnotationMapping
                            .getOrDefault(i, Collections.emptyList());
                    for (AnnotationInstance originalConstructorParameterAnnotation : originalConstructorParameterAnnotations) {
                        parameterAnnotations.addAnnotation(originalConstructorParameterAnnotation);
                    }
                }

                constructorCreator.invokeSpecialMethod(MethodDescriptor.of(originalConstructor), constructorCreator.getThis(), parametersRh.toArray(ResultHandle[]::new));
                constructorCreator.returnValue(null);
            }

            for (EventDispatchingMethod eventDispatchingMethod : eventDispatchingMethods) {
                AnnotationInstance eventSubscriberInstance = eventDispatchingMethod.getEventSubscriberInstance();
                MethodInfo originalMethod = eventDispatchingMethod.getMethod();
                Map<Short, List<AnnotationInstance>> originalMethodParameterAnnotationMapping = originalMethod.annotations().stream()
                        .filter(ai -> ai.target().kind() == Kind.METHOD_PARAMETER)
                        .collect(Collectors.groupingBy(ai -> ai.target().asMethodParameter().position()));

                // if the method already has an @Observes or @ObservesAsync annotation
                if (originalMethod.hasAnnotation(DotNames.OBSERVES) || originalMethod.hasAnnotation(DotNames.OBSERVES_ASYNC)) {
                    LOG.warn(
                            "Methods listening to GitHub events may not be annotated with @Observes or @ObservesAsync. Offending method: "
                                    + originalMethod.declaringClass().name() + "#" + originalMethod);
                }

                List<String> parameterTypes = new ArrayList<>();
                List<Type> originalMethodParameterTypes = originalMethod.parameters();
                short j = 0;
                Map<Short, Short> parameterMapping = new HashMap<>();
                for (short i = 0; i < originalMethodParameterTypes.size(); i++) {
                    List<AnnotationInstance> originalMethodAnnotations = originalMethodParameterAnnotationMapping
                            .getOrDefault(i, Collections.emptyList());
                    if (originalMethodAnnotations.stream().anyMatch(ai -> CONFIG_FILE.equals(ai.name()))) {
                        // if the parameter is annotated with @ConfigFile, we skip it
                        continue;
                    }

                    parameterTypes.add(originalMethodParameterTypes.get(i).name().toString());
                    parameterMapping.put(i, j);
                    j++;
                }
                if (originalMethod.hasAnnotation(CONFIG_FILE)) {
                    parameterTypes.add(ConfigFileReader.class.getName());
                }

                MethodCreator methodCreator = multiplexerClassCreator.getMethodCreator(
                        originalMethod.name() + "_" + HashUtil.sha1(eventSubscriberInstance.toString()),
                        originalMethod.returnType().name().toString(),
                        parameterTypes.toArray());

                ResultHandle[] parameterValues = new ResultHandle[originalMethod.parameters().size()];

                // detect the parameter that is a payload
                short payloadParameterPosition = 0;
                for (short i = 0; i < originalMethodParameterTypes.size(); i++) {
                    List<AnnotationInstance> parameterAnnotations = originalMethodParameterAnnotationMapping.getOrDefault(i,
                            Collections.emptyList());
                    if (parameterAnnotations.stream().anyMatch(ai -> ai.name().equals(eventSubscriberInstance.name()))) {
                        payloadParameterPosition = i;
                        break;
                    }
                }

                for (short i = 0; i < originalMethodParameterTypes.size(); i++) {
                    List<AnnotationInstance> parameterAnnotations = originalMethodParameterAnnotationMapping.getOrDefault(i,
                            Collections.emptyList());
                    AnnotatedElement generatedParameterAnnotations = methodCreator.getParameterAnnotations(i);
                    if (parameterAnnotations.stream().anyMatch(ai -> ai.name().equals(eventSubscriberInstance.name()))) {
                        generatedParameterAnnotations.addAnnotation(DotNames.OBSERVES_ASYNC.toString());
                        generatedParameterAnnotations.addAnnotation(eventSubscriberInstance);
                        parameterValues[i] = methodCreator.getMethodParam(parameterMapping.get(i));
                    } else if (parameterAnnotations.stream().anyMatch(ai -> ai.name().equals(CONFIG_FILE))) {
                        AnnotationInstance configFileAnnotationInstance = parameterAnnotations.stream()
                                .filter(ai -> ai.name().equals(CONFIG_FILE)).findFirst().get();
                        String configObjectType = originalMethodParameterTypes.get(i).name().toString();
                        // it's a config file, we will use the ConfigFileReader (last parameter of the method) and inject the result
                        ResultHandle configFileReaderRh = methodCreator.getMethodParam(parameterTypes.size() - 1);
                        ResultHandle ghRepositoryRh = methodCreator.invokeStaticMethod(MethodDescriptor
                                .ofMethod(PayloadHelper.class, "getRepository", GHRepository.class, GHEventPayload.class),
                                methodCreator.getMethodParam(payloadParameterPosition));
                        ResultHandle configObject = methodCreator.invokeVirtualMethod(
                                MethodDescriptor.ofMethod(ConfigFileReader.class, "getConfigObject", Object.class,
                                        GHRepository.class, String.class, Class.class),
                                configFileReaderRh,
                                ghRepositoryRh,
                                methodCreator.load(configFileAnnotationInstance.value().asString()),
                                methodCreator.loadClass(configObjectType));
                        parameterValues[i] = methodCreator.checkCast(configObject, configObjectType);
                    } else {
                        for (AnnotationInstance annotationInstance : parameterAnnotations) {
                            generatedParameterAnnotations.addAnnotation(annotationInstance);
                        }
                        parameterValues[i] = methodCreator.getMethodParam(parameterMapping.get(i));
                    }
                }

                ResultHandle returnValue = methodCreator.invokeVirtualMethod(originalMethod, methodCreator.getThis(), parameterValues);
                methodCreator.returnValue(returnValue);
            }

            multiplexerClassCreator.close();
        }
    }

    private static String getLiteralClassName(DotName annotationName) {
        return annotationName + "_AnnotationLiteral";
    }

    @SuppressWarnings("unused")
    private static void systemOutPrintln(BytecodeCreator bytecodeCreator, ResultHandle resultHandle) {
        bytecodeCreator.invokeVirtualMethod(MethodDescriptor.ofMethod(PrintStream.class, "println", void.class, String.class),
                bytecodeCreator.readStaticField(FieldDescriptor.of(System.class, "out", PrintStream.class)),
                bytecodeCreator.invokeVirtualMethod(MethodDescriptor.ofMethod(Object.class, "toString", String.class),
                        resultHandle));
    }
}
