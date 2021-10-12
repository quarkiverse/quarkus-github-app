package io.quarkiverse.githubapp.deployment;

import static io.quarkiverse.githubapp.deployment.GitHubAppDotNames.CONFIG_FILE;
import static io.quarkiverse.githubapp.deployment.GitHubAppDotNames.EVENT;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
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

import io.quarkiverse.githubapp.GitHubEvent;
import io.quarkiverse.githubapp.deployment.DispatchingConfiguration.EventAnnotation;
import io.quarkiverse.githubapp.deployment.DispatchingConfiguration.EventAnnotationLiteral;
import io.quarkiverse.githubapp.deployment.DispatchingConfiguration.EventDispatchingConfiguration;
import io.quarkiverse.githubapp.deployment.DispatchingConfiguration.EventDispatchingMethod;
import io.quarkiverse.githubapp.event.Actions;
import io.quarkiverse.githubapp.runtime.ConfigFileReader;
import io.quarkiverse.githubapp.runtime.GitHubAppRecorder;
import io.quarkiverse.githubapp.runtime.Multiplexer;
import io.quarkiverse.githubapp.runtime.error.DefaultErrorHandler;
import io.quarkiverse.githubapp.runtime.error.ErrorHandlerBridgeFunction;
import io.quarkiverse.githubapp.runtime.github.GitHubService;
import io.quarkiverse.githubapp.runtime.github.PayloadHelper;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.arc.processor.MethodDescriptors;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.LiveReloadBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.util.WebJarUtil;
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
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.util.HashUtil;
import io.quarkus.vertx.http.deployment.HttpRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.deployment.devmode.NotFoundPageDisplayableEndpointBuildItem;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

class GitHubAppProcessor {

    private static final Logger LOG = Logger.getLogger(GitHubAppProcessor.class);

    private static final String FEATURE = "github-app";

    private static final String EVENT_EMITTER_FIELD = "eventEmitter";
    private static final String GITHUB_SERVICE_FIELD = "gitHubService";

    private static final MethodDescriptor EVENT_SELECT = MethodDescriptor.ofMethod(Event.class, "select", Event.class,
            Annotation[].class);
    private static final MethodDescriptor EVENT_FIRE_ASYNC = MethodDescriptor.ofMethod(Event.class, "fireAsync",
            CompletionStage.class, Object.class);
    private static final MethodDescriptor COMPLETION_STAGE_EXCEPTIONALLY = MethodDescriptor.ofMethod(CompletionStage.class,
            "exceptionally", CompletionStage.class, Function.class);
    private static final MethodDescriptor COMPLETION_STAGE_TO_COMPLETABLE_FUTURE = MethodDescriptor.ofMethod(
            CompletionStage.class,
            "toCompletableFuture", CompletableFuture.class);
    private static final MethodDescriptor COMPLETABLE_FUTURE_JOIN = MethodDescriptor.ofMethod(CompletableFuture.class,
            "join", Object.class);

    private static final String QUARKIVERSE_GITHUB_APP_GROUP_ID = "io.quarkiverse.githubapp";
    private static final String QUARKIVERSE_GITHUB_APP_ARTIFACT_ID = "quarkus-github-app-deployment";
    private static final String REPLAY_UI_RESOURCES_PREFIX = "META-INF/resources/replay-ui/";
    private static final String REPLAY_UI_PATH = "/replay";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
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
                    .source(GitHubAppProcessor.class.getSimpleName() + " > " + methodParameter.method().declaringClass() + "#"
                            + methodParameter.method())
                    .build());
        }

        // Caffeine
        reflectiveClasses.produce(new ReflectiveClassBuildItem(true, true,
                "com.github.benmanes.caffeine.cache.SSMSA",
                "com.github.benmanes.caffeine.cache.PSWMS"));
    }

    @BuildStep
    void additionalBeans(CombinedIndexBuildItem index, BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        AdditionalBeanBuildItem.Builder additionalBeanBuildItemBuilder = new AdditionalBeanBuildItem.Builder().addBeanClasses(
                GitHubService.class,
                DefaultErrorHandler.class)
                .setUnremovable()
                .setDefaultScope(DotNames.SINGLETON);

        for (ClassInfo errorHandler : index.getIndex().getAllKnownImplementors(GitHubAppDotNames.ERROR_HANDLER)) {
            additionalBeanBuildItemBuilder.addBeanClass(errorHandler.name().toString());
        }

        additionalBeans.produce(additionalBeanBuildItemBuilder.build());
    }

    @BuildStep
    void generateClasses(CombinedIndexBuildItem combinedIndex, LaunchModeBuildItem launchMode,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<GeneratedBeanBuildItem> generatedBeans,
            BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            BuildProducer<AnnotationsTransformerBuildItem> annotationsTransformer) {
        Collection<EventDefinition> allEventDefinitions = getAllEventDefinitions(combinedIndex.getIndex());

        // Add @Vetoed to all the user-defined event listening classes
        annotationsTransformer
                .produce(new AnnotationsTransformerBuildItem(new VetoUserDefinedEventListeningClassesAnnotationsTransformer(
                        allEventDefinitions.stream().map(d -> d.getAnnotation()).collect(Collectors.toSet()))));

        // Add the qualifiers as beans
        String[] subscriberAnnotations = allEventDefinitions.stream().map(d -> d.getAnnotation().toString())
                .toArray(String[]::new);
        additionalBeans.produce(new AdditionalBeanBuildItem(subscriberAnnotations));

        DispatchingConfiguration dispatchingConfiguration = getDispatchingConfiguration(
                combinedIndex.getIndex(), allEventDefinitions);

        ClassOutput classOutput = new GeneratedClassGizmoAdaptor(generatedClasses, true);
        generateAnnotationLiterals(classOutput, dispatchingConfiguration);

        ClassOutput beanClassOutput = new GeneratedBeanGizmoAdaptor(generatedBeans);
        generateDispatcher(beanClassOutput, combinedIndex, launchMode, dispatchingConfiguration, reflectiveClasses);
        generateMultiplexers(beanClassOutput, dispatchingConfiguration, reflectiveClasses);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void replayUi(GitHubAppRecorder recorder,
            LaunchModeBuildItem launchMode,
            LiveReloadBuildItem liveReloadBuildItem,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            HttpRootPathBuildItem httpRootPathBuildItem,
            BuildProducer<RouteBuildItem> routes,
            BuildProducer<NotFoundPageDisplayableEndpointBuildItem> displayableEndpoints,
            ShutdownContextBuildItem shutdownContext) throws IOException {
        if (launchMode.getLaunchMode() != LaunchMode.DEVELOPMENT) {
            return;
        }

        ResolvedDependency githubAppArtifact = WebJarUtil.getAppArtifact(curateOutcomeBuildItem,
                QUARKIVERSE_GITHUB_APP_GROUP_ID,
                QUARKIVERSE_GITHUB_APP_ARTIFACT_ID);
        Path deploymentPath = WebJarUtil.copyResourcesForDevOrTest(liveReloadBuildItem, curateOutcomeBuildItem, launchMode,
                githubAppArtifact, REPLAY_UI_RESOURCES_PREFIX);

        Handler<RoutingContext> handler = recorder.replayUiHandler(deploymentPath.toAbsolutePath().toString(),
                REPLAY_UI_PATH, shutdownContext);
        routes.produce(httpRootPathBuildItem.routeBuilder().route(REPLAY_UI_PATH).handler(handler).build());
        routes.produce(httpRootPathBuildItem.routeBuilder().route(REPLAY_UI_PATH + "/*").handler(handler).build());

        displayableEndpoints.produce(new NotFoundPageDisplayableEndpointBuildItem(REPLAY_UI_PATH + "/"));
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
                DotName annotatedParameterType = annotatedParameter.method().parameters().get(annotatedParameter.position())
                        .name();
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

    /**
     * The role of the dispatcher is to receive the CDI events emitted by the reactive route.
     * <p>
     * It parses the raw payload into the appropriate {@link GHEventPayload} and then emit
     * an async CDI event with the payload instance.
     * <p>
     * It only generates code for the GitHub events actually listened to by the application.
     */
    private static void generateDispatcher(ClassOutput beanClassOutput,
            CombinedIndexBuildItem combinedIndex,
            LaunchModeBuildItem launchMode,
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

        FieldCreator gitHubServiceFieldCreator = dispatcherClassCreator.getFieldCreator(GITHUB_SERVICE_FIELD,
                GitHubService.class);
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
                    fireAsyncAction(eventMatchesCreator, launchMode.getLaunchMode(), dispatcherClassCreator.getClassName(),
                            gitHubEventRh,
                            payloadInstanceRh, annotationLiteralArrayRh);
                } else {
                    BytecodeCreator actionMatchesCreator = eventMatchesCreator
                            .ifTrue(eventMatchesCreator.invokeVirtualMethod(MethodDescriptors.OBJECT_EQUALS,
                                    eventMatchesCreator.load(action), dispatchedActionRh))
                            .trueBranch();

                    fireAsyncAction(actionMatchesCreator, launchMode.getLaunchMode(), dispatcherClassCreator.getClassName(),
                            gitHubEventRh,
                            payloadInstanceRh, annotationLiteralArrayRh);
                }
            }
        }

        CatchBlockCreator catchBlockCreator = tryBlock.addCatch(Throwable.class);
        catchBlockCreator.invokeVirtualMethod(
                MethodDescriptor.ofMethod(ErrorHandlerBridgeFunction.class, "apply", Void.class, Throwable.class),
                catchBlockCreator.newInstance(
                        MethodDescriptor.ofConstructor(ErrorHandlerBridgeFunction.class, GitHubEvent.class), gitHubEventRh),
                catchBlockCreator.getCaughtException());

        dispatchMethodCreator.returnValue(null);

        dispatcherClassCreator.close();
    }

    /**
     * Multiplexers listen to the async events emitted by the dispatcher.
     * <p>
     * They are subclasses of the application classes listening to GitHub events through our annotations.
     * <p>
     * They are useful for two purposes:
     * <ul>
     * <li>A single application method can listen to multiple event types: the event types are qualifiers and CDI wouldn't allow
     * that (only events matching all the qualifiers would be received by the application method). That's why this class is
     * called a multiplexer: it will generate one method per event type and each generated method will delegate to the original
     * method.</li>
     * <li>The multiplexer also handles the resolution of config files.</li>
     * </ul>
     */
    private static void generateMultiplexers(ClassOutput beanClassOutput,
            DispatchingConfiguration dispatchingConfiguration,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        for (Entry<DotName, TreeSet<EventDispatchingMethod>> eventDispatchingMethodsEntry : dispatchingConfiguration
                .getMethods().entrySet()) {
            DotName declaringClassName = eventDispatchingMethodsEntry.getKey();
            TreeSet<EventDispatchingMethod> eventDispatchingMethods = eventDispatchingMethodsEntry.getValue();
            ClassInfo declaringClass = eventDispatchingMethods.iterator().next().getMethod().declaringClass();

            reflectiveClasses.produce(new ReflectiveClassBuildItem(true, true, declaringClassName.toString()));

            String multiplexerClassName = declaringClassName + "_Multiplexer";
            reflectiveClasses.produce(new ReflectiveClassBuildItem(true, true, multiplexerClassName));

            ClassCreator multiplexerClassCreator = ClassCreator.builder().classOutput(beanClassOutput)
                    .className(multiplexerClassName)
                    .superClass(declaringClassName.toString())
                    .build();

            multiplexerClassCreator.addAnnotation(Multiplexer.class);

            if (!BuiltinScope.isDeclaredOn(declaringClass)) {
                multiplexerClassCreator.addAnnotation(Singleton.class);
            }

            for (AnnotationInstance classAnnotation : declaringClass.classAnnotations()) {
                multiplexerClassCreator.addAnnotation(classAnnotation);
            }

            // Copy the constructors
            for (MethodInfo originalConstructor : declaringClass.constructors()) {
                MethodCreator constructorCreator = multiplexerClassCreator.getMethodCreator(MethodDescriptor.ofConstructor(
                        multiplexerClassName,
                        originalConstructor.parameters().stream().map(t -> t.name().toString()).toArray(String[]::new)));

                List<AnnotationInstance> originalMethodAnnotations = originalConstructor.annotations().stream()
                        .filter(ai -> ai.target().kind() == Kind.METHOD).collect(Collectors.toList());
                for (AnnotationInstance originalMethodAnnotation : originalMethodAnnotations) {
                    constructorCreator.addAnnotation(originalMethodAnnotation);
                }

                Map<Short, List<AnnotationInstance>> originalConstructorParameterAnnotationMapping = originalConstructor
                        .annotations().stream()
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

                constructorCreator.invokeSpecialMethod(MethodDescriptor.of(originalConstructor), constructorCreator.getThis(),
                        parametersRh.toArray(ResultHandle[]::new));
                constructorCreator.returnValue(null);
            }

            // Generate the multiplexed event dispatching methods
            for (EventDispatchingMethod eventDispatchingMethod : eventDispatchingMethods) {
                AnnotationInstance eventSubscriberInstance = eventDispatchingMethod.getEventSubscriberInstance();
                MethodInfo originalMethod = eventDispatchingMethod.getMethod();
                Map<Short, List<AnnotationInstance>> originalMethodParameterAnnotationMapping = originalMethod.annotations()
                        .stream()
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

                for (Type exceptionType : originalMethod.exceptions()) {
                    methodCreator.addException(exceptionType.name().toString());
                }

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

                // copy annotations except for @ConfigFile
                for (short i = 0; i < originalMethodParameterTypes.size(); i++) {
                    List<AnnotationInstance> parameterAnnotations = originalMethodParameterAnnotationMapping.getOrDefault(i,
                            Collections.emptyList());
                    if (parameterAnnotations.isEmpty()) {
                        continue;
                    }

                    // @ConfigFile elements are not in the mapping
                    Short generatedParameterIndex = parameterMapping.get(i);
                    if (generatedParameterIndex == null) {
                        continue;
                    }

                    AnnotatedElement generatedParameterAnnotations = methodCreator
                            .getParameterAnnotations(generatedParameterIndex);
                    if (parameterAnnotations.stream().anyMatch(ai -> ai.name().equals(eventSubscriberInstance.name()))) {
                        generatedParameterAnnotations.addAnnotation(DotNames.OBSERVES_ASYNC.toString());
                        generatedParameterAnnotations.addAnnotation(eventSubscriberInstance);
                    } else {
                        for (AnnotationInstance annotationInstance : parameterAnnotations) {
                            generatedParameterAnnotations.addAnnotation(annotationInstance);
                        }
                    }
                }

                // generate the code of the method
                for (short originalMethodParameterIndex = 0; originalMethodParameterIndex < originalMethodParameterTypes
                        .size(); originalMethodParameterIndex++) {
                    List<AnnotationInstance> parameterAnnotations = originalMethodParameterAnnotationMapping.getOrDefault(
                            originalMethodParameterIndex,
                            Collections.emptyList());
                    Short multiplexerMethodParameterIndex = parameterMapping.get(originalMethodParameterIndex);
                    if (parameterAnnotations.stream().anyMatch(ai -> ai.name().equals(eventSubscriberInstance.name()))) {
                        parameterValues[originalMethodParameterIndex] = methodCreator
                                .getMethodParam(multiplexerMethodParameterIndex);
                    } else if (parameterAnnotations.stream().anyMatch(ai -> ai.name().equals(CONFIG_FILE))) {
                        AnnotationInstance configFileAnnotationInstance = parameterAnnotations.stream()
                                .filter(ai -> ai.name().equals(CONFIG_FILE)).findFirst().get();
                        String configObjectType = originalMethodParameterTypes.get(originalMethodParameterIndex).name()
                                .toString();

                        boolean isOptional = false;
                        if (Optional.class.getName().equals(configObjectType)) {
                            if (originalMethodParameterTypes.get(originalMethodParameterIndex)
                                    .kind() != Type.Kind.PARAMETERIZED_TYPE) {
                                throw new IllegalStateException("Optional is used but not parameterized for method " +
                                        originalMethod.declaringClass().name() + "#" + originalMethod);
                            }
                            isOptional = true;
                            configObjectType = originalMethodParameterTypes.get(originalMethodParameterIndex)
                                    .asParameterizedType().arguments().get(0)
                                    .name().toString();
                        }

                        // it's a config file, we will use the ConfigFileReader (last parameter of the method) and inject the result
                        ResultHandle configFileReaderRh = methodCreator.getMethodParam(parameterTypes.size() - 1);
                        ResultHandle ghRepositoryRh = methodCreator.invokeStaticMethod(MethodDescriptor
                                .ofMethod(PayloadHelper.class, "getRepository", GHRepository.class, GHEventPayload.class),
                                methodCreator.getMethodParam(parameterMapping.get(payloadParameterPosition)));
                        ResultHandle configObject = methodCreator.invokeVirtualMethod(
                                MethodDescriptor.ofMethod(ConfigFileReader.class, "getConfigObject", Object.class,
                                        GHRepository.class, String.class, Class.class),
                                configFileReaderRh,
                                ghRepositoryRh,
                                methodCreator.load(configFileAnnotationInstance.value().asString()),
                                methodCreator.loadClass(configObjectType));
                        configObject = methodCreator.checkCast(configObject, configObjectType);

                        if (isOptional) {
                            configObject = methodCreator.invokeStaticMethod(
                                    MethodDescriptor.ofMethod(Optional.class, "ofNullable", Optional.class, Object.class),
                                    configObject);
                        }

                        parameterValues[originalMethodParameterIndex] = configObject;
                    } else {
                        parameterValues[originalMethodParameterIndex] = methodCreator
                                .getMethodParam(multiplexerMethodParameterIndex);
                    }
                }

                ResultHandle returnValue = methodCreator.invokeVirtualMethod(originalMethod, methodCreator.getThis(),
                        parameterValues);
                methodCreator.returnValue(returnValue);
            }

            multiplexerClassCreator.close();
        }
    }

    private static ResultHandle fireAsyncAction(BytecodeCreator bytecodeCreator, LaunchMode launchMode, String className,
            ResultHandle gitHubEventRh, ResultHandle payloadInstanceRh, ResultHandle annotationLiteralArrayRh) {
        ResultHandle cdiEventRh = bytecodeCreator.invokeInterfaceMethod(EVENT_SELECT,
                bytecodeCreator.readInstanceField(
                        FieldDescriptor.of(className, EVENT_EMITTER_FIELD, Event.class),
                        bytecodeCreator.getThis()),
                annotationLiteralArrayRh);

        ResultHandle fireAsyncCompletionStageRH = bytecodeCreator.invokeInterfaceMethod(EVENT_FIRE_ASYNC, cdiEventRh,
                payloadInstanceRh);

        ResultHandle exceptionallyRH = bytecodeCreator.invokeInterfaceMethod(COMPLETION_STAGE_EXCEPTIONALLY,
                fireAsyncCompletionStageRH,
                bytecodeCreator.newInstance(
                        MethodDescriptor.ofConstructor(ErrorHandlerBridgeFunction.class, GitHubEvent.class,
                                GHEventPayload.class),
                        gitHubEventRh, payloadInstanceRh));

        if (LaunchMode.TEST.equals(launchMode)) {
            ResultHandle toFutureRH = bytecodeCreator.invokeInterfaceMethod(COMPLETION_STAGE_TO_COMPLETABLE_FUTURE,
                    exceptionallyRH);
            return bytecodeCreator.invokeVirtualMethod(COMPLETABLE_FUTURE_JOIN, toFutureRH);
        } else {
            return exceptionallyRH;
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
