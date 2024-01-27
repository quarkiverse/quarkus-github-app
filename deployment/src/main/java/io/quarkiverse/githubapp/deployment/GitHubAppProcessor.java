package io.quarkiverse.githubapp.deployment;

import static io.quarkiverse.githubapp.deployment.GitHubAppDotNames.CONFIG_FILE;
import static io.quarkiverse.githubapp.deployment.GitHubAppDotNames.DYNAMIC_GRAPHQL_CLIENT;
import static io.quarkiverse.githubapp.deployment.GitHubAppDotNames.EVENT;
import static io.quarkiverse.githubapp.deployment.GitHubAppDotNames.GITHUB;
import static io.quarkiverse.githubapp.deployment.GitHubAppDotNames.GITHUB_EVENT;
import static io.quarkus.gizmo.Type.classType;
import static io.quarkus.gizmo.Type.parameterizedType;

import java.io.IOException;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;
import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.github.benmanes.caffeine.cache.CacheLoader;

import io.quarkiverse.githubapp.ConfigFile;
import io.quarkiverse.githubapp.GitHubEvent;
import io.quarkiverse.githubapp.deployment.DispatchingConfiguration.EventAnnotation;
import io.quarkiverse.githubapp.deployment.DispatchingConfiguration.EventAnnotationLiteral;
import io.quarkiverse.githubapp.deployment.DispatchingConfiguration.EventDispatchingConfiguration;
import io.quarkiverse.githubapp.deployment.DispatchingConfiguration.EventDispatchingMethod;
import io.quarkiverse.githubapp.error.ErrorHandler;
import io.quarkiverse.githubapp.event.Actions;
import io.quarkiverse.githubapp.runtime.GitHubAppRecorder;
import io.quarkiverse.githubapp.runtime.MultiplexedEvent;
import io.quarkiverse.githubapp.runtime.Multiplexer;
import io.quarkiverse.githubapp.runtime.RequestScopeCachingGitHubConfigFileProvider;
import io.quarkiverse.githubapp.runtime.Routes;
import io.quarkiverse.githubapp.runtime.UtilsProducer;
import io.quarkiverse.githubapp.runtime.config.CheckedConfigProvider;
import io.quarkiverse.githubapp.runtime.error.DefaultErrorHandler;
import io.quarkiverse.githubapp.runtime.error.ErrorHandlerBridgeFunction;
import io.quarkiverse.githubapp.runtime.github.GitHubConfigFileProviderImpl;
import io.quarkiverse.githubapp.runtime.github.GitHubFileDownloader;
import io.quarkiverse.githubapp.runtime.github.GitHubService;
import io.quarkiverse.githubapp.runtime.github.PayloadHelper;
import io.quarkiverse.githubapp.runtime.replay.ReplayEvent;
import io.quarkiverse.githubapp.runtime.replay.ReplayEventsRoute;
import io.quarkiverse.githubapp.runtime.signing.JwtTokenCreator;
import io.quarkiverse.githubapp.runtime.signing.PayloadSignatureChecker;
import io.quarkiverse.githubapp.runtime.smee.SmeeIoForwarder;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.arc.processor.MethodDescriptors;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.gizmo.AnnotatedElement;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.CatchBlockCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.Gizmo;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.SignatureBuilder;
import io.quarkus.gizmo.TryBlock;
import io.quarkus.maven.dependency.GACT;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.util.HashUtil;
import io.quarkus.vertx.http.deployment.HttpRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.deployment.devmode.NotFoundPageDisplayableEndpointBuildItem;
import io.quarkus.vertx.http.deployment.webjar.WebJarBuildItem;
import io.quarkus.vertx.http.deployment.webjar.WebJarResultsBuildItem;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClient;
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
    private static final MethodDescriptor COMPLETION_STAGE_TO_COMPLETABLE_FUTURE = MethodDescriptor.ofMethod(
            CompletionStage.class,
            "toCompletableFuture", CompletableFuture.class);
    private static final MethodDescriptor COMPLETABLE_FUTURE_JOIN = MethodDescriptor.ofMethod(CompletableFuture.class,
            "join", Object.class);

    private static final DotName WITH_BRIDGE_METHODS = DotName
            .createSimple("com.infradna.tool.bridge_method_injector.WithBridgeMethods");

    private static final GACT QUARKIVERSE_GITHUB_APP_GACT = new GACT("io.quarkiverse.githubapp",
            "quarkus-github-app-deployment", null, "jar");
    private static final String REPLAY_UI_RESOURCES_PREFIX = "META-INF/resources/replay-ui/";
    private static final String REPLAY_UI_PATH = "/replay";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    AdditionalIndexedClassesBuildItem additionalIndexedClasses() {
        return new AdditionalIndexedClassesBuildItem(GitHubEvent.class.getName(),
                ReplayEvent.class.getName(),
                ConfigFile.class.getName());
    }

    @BuildStep
    void registerForReflection(CombinedIndexBuildItem combinedIndex,
            List<AdditionalEventDispatchingClassesIndexBuildItem> additionalEventDispatchingIndexes,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchies) {
        List<IndexView> indexes = new ArrayList<>();
        indexes.add(combinedIndex.getIndex());
        additionalEventDispatchingIndexes.forEach(i -> indexes.add(i.getIndex()));
        IndexView index = CompositeIndex.create(indexes);

        // Types used for config files
        for (AnnotationInstance configFileAnnotationInstance : index.getAnnotations(CONFIG_FILE)) {
            MethodParameterInfo methodParameter = configFileAnnotationInstance.target().asMethodParameter();
            short parameterPosition = methodParameter.position();
            Type parameterType = methodParameter.method().parameterTypes().get(parameterPosition);
            reflectiveHierarchies.produce(new ReflectiveHierarchyBuildItem.Builder()
                    .type(parameterType)
                    .index(index)
                    .source(GitHubAppProcessor.class.getSimpleName() + " > " + methodParameter.method().declaringClass() + "#"
                            + methodParameter.method())
                    .build());
        }

        // Caffeine
        reflectiveClasses.produce(ReflectiveClassBuildItem
                .builder("com.github.benmanes.caffeine.cache.SSMSA", "com.github.benmanes.caffeine.cache.PSWMS")
                .methods(true)
                .fields(true)
                .build());
        reflectiveClasses.produce(ReflectiveClassBuildItem
                .builder(CacheLoader.class)
                .methods(true)
                .build());
        reflectiveClasses.produce(ReflectiveClassBuildItem
                .builder(GitHubService.class.getName() + "$CreateInstallationToken")
                .methods(true)
                .build());
    }

    @BuildStep
    void additionalBeans(CombinedIndexBuildItem index, BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        AdditionalBeanBuildItem.Builder additionalBeanBuildItemBuilder = new AdditionalBeanBuildItem.Builder().addBeanClasses(
                Routes.class,
                UtilsProducer.class,
                RequestScopeCachingGitHubConfigFileProvider.class,
                Multiplexer.class,
                SmeeIoForwarder.class,
                PayloadSignatureChecker.class,
                JwtTokenCreator.class,
                GitHubService.class,
                DefaultErrorHandler.class,
                GitHubFileDownloader.class,
                GitHubConfigFileProviderImpl.class,
                CheckedConfigProvider.class)
                .setUnremovable();

        for (ClassInfo errorHandler : index.getIndex().getAllKnownImplementors(GitHubAppDotNames.ERROR_HANDLER)) {
            additionalBeanBuildItemBuilder.addBeanClass(errorHandler.name().toString());
        }

        additionalBeans.produce(additionalBeanBuildItemBuilder.build());
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    void additionalBeansDevMode(CombinedIndexBuildItem index, BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        AdditionalBeanBuildItem.Builder additionalBeanBuildItemBuilder = new AdditionalBeanBuildItem.Builder().addBeanClasses(
                ReplayEventsRoute.class)
                .setUnremovable();

        additionalBeans.produce(additionalBeanBuildItemBuilder.build());
    }

    /**
     * The bridge methods added for binary compatibility in the GitHub API are causing issues with Mockito
     * and more specifically with Byte Buddy (see https://github.com/raphw/byte-buddy/issues/1162).
     * They don't bring much to the plate for new applications that are regularly updated so let's remove them altogether.
     */
    @BuildStep
    void removeCompatibilityBridgeMethodsFromGitHubApi(
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<BytecodeTransformerBuildItem> bytecodeTransformers) {
        Map<String, Set<String>> bridgeMethodsByClassName = new HashMap<>();

        for (AnnotationInstance bridgeAnnotation : combinedIndex.getIndex().getAnnotations(WITH_BRIDGE_METHODS)) {
            if (bridgeAnnotation.target().kind() != Kind.METHOD) {
                continue;
            }

            String className = bridgeAnnotation.target().asMethod().declaringClass().name().toString();
            bridgeMethodsByClassName.computeIfAbsent(className, cn -> new HashSet<>())
                    .add(bridgeAnnotation.target().asMethod().name());
        }

        for (Entry<String, Set<String>> bridgeMethodsByClassNameEntry : bridgeMethodsByClassName.entrySet()) {
            bytecodeTransformers.produce(new BytecodeTransformerBuildItem.Builder()
                    .setClassToTransform(bridgeMethodsByClassNameEntry.getKey())
                    .setVisitorFunction((ignored, visitor) -> new RemoveBridgeMethodsClassVisitor(visitor,
                            bridgeMethodsByClassNameEntry.getKey(),
                            bridgeMethodsByClassNameEntry.getValue()))
                    .build());
        }
    }

    @BuildStep
    void generateClasses(CombinedIndexBuildItem combinedIndex, LaunchModeBuildItem launchMode,
            List<AdditionalEventDispatchingClassesIndexBuildItem> additionalEventDispatchingIndexes,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<GeneratedBeanBuildItem> generatedBeans,
            BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            BuildProducer<AnnotationsTransformerBuildItem> annotationsTransformer) {
        List<IndexView> indexes = new ArrayList<>();
        indexes.add(combinedIndex.getIndex());
        additionalEventDispatchingIndexes.forEach(i -> indexes.add(i.getIndex()));
        IndexView index = CompositeIndex.create(indexes);

        Collection<EventDefinition> allEventDefinitions = getAllEventDefinitions(index);

        // Add @Vetoed to all the user-defined event listening classes
        annotationsTransformer
                .produce(new AnnotationsTransformerBuildItem(new VetoUserDefinedEventListeningClassesAnnotationsTransformer(
                        allEventDefinitions.stream().map(d -> d.getAnnotation()).collect(Collectors.toSet()))));

        // Add the qualifiers as beans
        String[] subscriberAnnotations = allEventDefinitions.stream().map(d -> d.getAnnotation().toString())
                .toArray(String[]::new);
        additionalBeans.produce(new AdditionalBeanBuildItem(subscriberAnnotations));

        DispatchingConfiguration dispatchingConfiguration = getDispatchingConfiguration(
                index, allEventDefinitions);

        ClassOutput classOutput = new GeneratedClassGizmoAdaptor(generatedClasses, true);
        generateAnnotationLiterals(classOutput, dispatchingConfiguration);

        ClassOutput beanClassOutput = new GeneratedBeanGizmoAdaptor(generatedBeans);
        generateDispatcher(beanClassOutput, launchMode, dispatchingConfiguration, reflectiveClasses);
        generateMultiplexers(beanClassOutput, index, dispatchingConfiguration, reflectiveClasses);
    }

    @BuildStep
    void replayUiDeployment(LaunchModeBuildItem launchMode,
            BuildProducer<WebJarBuildItem> webJars) throws IOException {
        if (launchMode.getLaunchMode() != LaunchMode.DEVELOPMENT) {
            return;
        }

        webJars.produce(WebJarBuildItem.builder()
                .artifactKey(QUARKIVERSE_GITHUB_APP_GACT)
                .root(REPLAY_UI_RESOURCES_PREFIX)
                .build());
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void replayUi(GitHubAppRecorder recorder,
            LaunchModeBuildItem launchMode,
            WebJarResultsBuildItem webJarResults,
            HttpRootPathBuildItem httpRootPath,
            ShutdownContextBuildItem shutdownContext,
            BuildProducer<RouteBuildItem> routes,
            BuildProducer<NotFoundPageDisplayableEndpointBuildItem> displayableEndpoints) throws IOException {
        if (launchMode.getLaunchMode() != LaunchMode.DEVELOPMENT) {
            return;
        }

        WebJarResultsBuildItem.WebJarResult webJarResult = webJarResults.byArtifactKey(QUARKIVERSE_GITHUB_APP_GACT);
        if (webJarResult == null) {
            return;
        }

        Handler<RoutingContext> handler = recorder.replayUiHandler(webJarResult.getFinalDestination(), REPLAY_UI_PATH,
                webJarResult.getWebRootConfigurations(), shutdownContext);
        routes.produce(httpRootPath.routeBuilder().route(REPLAY_UI_PATH).handler(handler).build());
        routes.produce(httpRootPath.routeBuilder().route(REPLAY_UI_PATH + "/*").handler(handler).build());

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
                    .filter(ai -> !Modifier.isInterface(ai.target().asMethodParameter().method().declaringClass().flags()))
                    .collect(Collectors.toList());
            for (AnnotationInstance eventSubscriberInstance : eventSubscriberInstances) {
                String action = eventDefinition.getAction() != null ? eventDefinition.getAction()
                        : (eventSubscriberInstance.value() != null ? eventSubscriberInstance.value().asString() : Actions.ALL);

                MethodParameterInfo annotatedParameter = eventSubscriberInstance.target().asMethodParameter();
                MethodInfo methodInfo = annotatedParameter.method();
                DotName annotatedParameterType = annotatedParameter.method().parameterTypes().get(annotatedParameter.position())
                        .name();
                if (!eventDefinition.getPayloadType().equals(annotatedParameterType)) {
                    throw new IllegalStateException(
                            "Parameter subscribing to a GitHub '" + eventDefinition.getEvent()
                                    + "' event should be of type '" + eventDefinition.getPayloadType()
                                    + "'. Offending method: " + methodInfo.declaringClass().name() + "#" + methodInfo);
                }

                configuration
                        .getOrCreateEventConfiguration(eventDefinition.getEvent(), eventDefinition.getPayloadType().toString())
                        .addEventAnnotation(action, eventSubscriberInstance, eventSubscriberInstance.valuesWithDefaults(index));
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

                String signature = SignatureBuilder.forClass()
                        .setSuperClass(parameterizedType(classType(AnnotationLiteral.class),
                                classType(eventAnnotationLiteral.getName())))
                        .addInterface(classType(eventAnnotationLiteral.getName()))
                        .build();

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
     * an async CDI event with a MultiplexedEvent containing the payload instance,
     * the GitHub instance and the DynamicGraphQLClient instance if needed.
     * <p>
     * It only generates code for the GitHub events actually listened to by the application.
     */
    private static void generateDispatcher(ClassOutput beanClassOutput,
            LaunchModeBuildItem launchMode,
            DispatchingConfiguration dispatchingConfiguration,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        String dispatcherClassName = GitHubEvent.class.getName() + "DispatcherImpl";

        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(dispatcherClassName).methods(true).fields(true).build());

        ClassCreator dispatcherClassCreator = ClassCreator.builder().classOutput(beanClassOutput)
                .className(dispatcherClassName)
                .build();

        dispatcherClassCreator.addAnnotation(Singleton.class);

        FieldCreator eventFieldCreator = dispatcherClassCreator.getFieldCreator(EVENT_EMITTER_FIELD, Event.class);
        eventFieldCreator.addAnnotation(Inject.class);
        eventFieldCreator.setModifiers(Modifier.PROTECTED);
        eventFieldCreator.setSignature(SignatureBuilder.forField()
                .setType(parameterizedType(classType(Event.class), classType(MultiplexedEvent.class)))
                .build());

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
                MethodDescriptor.ofMethod(GitHubService.class, "getInstallationClient", GitHub.class, long.class),
                tryBlock.readInstanceField(
                        FieldDescriptor.of(dispatcherClassCreator.getClassName(), GITHUB_SERVICE_FIELD, GitHubService.class),
                        tryBlock.getThis()),
                installationIdRh);

        ResultHandle gitHubGraphQLClientRh = tryBlock.loadNull();

        if (dispatchingConfiguration.requiresGraphQLClient()) {
            gitHubGraphQLClientRh = tryBlock.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(GitHubService.class, "getInstallationGraphQLClient", DynamicGraphQLClient.class,
                            long.class),
                    tryBlock.readInstanceField(
                            FieldDescriptor.of(dispatcherClassCreator.getClassName(), GITHUB_SERVICE_FIELD,
                                    GitHubService.class),
                            tryBlock.getThis()),
                    installationIdRh);
        }

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

            ResultHandle multiplexedEventRh = eventMatchesCreator.newInstance(MethodDescriptor
                    .ofConstructor(MultiplexedEvent.class, GitHubEvent.class, GHEventPayload.class, GitHub.class,
                            DynamicGraphQLClient.class),
                    gitHubEventRh, payloadInstanceRh, gitHubRh, gitHubGraphQLClientRh);

            for (Entry<String, EventAnnotation> eventAnnotationEntry : eventDispatchingConfiguration.getEventAnnotations()
                    .entrySet()) {
                String action = eventAnnotationEntry.getKey();
                EventAnnotation eventAnnotation = eventAnnotationEntry.getValue();

                Class<?>[] literalParameterTypes = new Class<?>[eventAnnotation.getValues().size()];
                Arrays.fill(literalParameterTypes, String.class);
                List<ResultHandle> literalParameters = new ArrayList<>();
                for (AnnotationValue eventAnnotationValue : eventAnnotation.getValues()) {
                    literalParameters.add(eventMatchesCreator.load(eventAnnotationValue.asString()));
                }

                ResultHandle annotationLiteralRh = eventMatchesCreator.newInstance(MethodDescriptor
                        .ofConstructor(getLiteralClassName(eventAnnotation.getName()), (Object[]) literalParameterTypes),
                        literalParameters.toArray(ResultHandle[]::new));
                ResultHandle annotationLiteralArrayRh = eventMatchesCreator.newArray(Annotation.class, 1);
                eventMatchesCreator.writeArrayValue(annotationLiteralArrayRh, 0, annotationLiteralRh);

                if (Actions.ALL.equals(action)) {
                    fireAsyncAction(eventMatchesCreator, launchMode.getLaunchMode(), dispatcherClassCreator.getClassName(),
                            gitHubEventRh, multiplexedEventRh, annotationLiteralArrayRh);
                } else {
                    BytecodeCreator actionMatchesCreator = eventMatchesCreator
                            .ifTrue(eventMatchesCreator.invokeVirtualMethod(MethodDescriptors.OBJECT_EQUALS,
                                    eventMatchesCreator.load(action), dispatchedActionRh))
                            .trueBranch();

                    fireAsyncAction(actionMatchesCreator, launchMode.getLaunchMode(), dispatcherClassCreator.getClassName(),
                            gitHubEventRh, multiplexedEventRh, annotationLiteralArrayRh);
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
     * They are useful for several purposes:
     * <ul>
     * <li>A single application method can listen to multiple event types: the event types are qualifiers and CDI wouldn't allow
     * that (only events matching all the qualifiers would be received by the application method). That's why this class is
     * called a multiplexer: it will generate one method per event type and each generated method will delegate to the original
     * method.</li>
     * <li>The multiplexer also handles the resolution of config files.</li>
     * <li>We can inject a properly configured instance of GitHub or DynamicGraphQLClient into the method.</li>
     * </ul>
     */
    private static void generateMultiplexers(ClassOutput beanClassOutput,
            IndexView index,
            DispatchingConfiguration dispatchingConfiguration,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        for (Entry<DotName, TreeSet<EventDispatchingMethod>> eventDispatchingMethodsEntry : dispatchingConfiguration
                .getMethods().entrySet()) {
            DotName declaringClassName = eventDispatchingMethodsEntry.getKey();
            TreeSet<EventDispatchingMethod> eventDispatchingMethods = eventDispatchingMethodsEntry.getValue();
            ClassInfo declaringClass = eventDispatchingMethods.iterator().next().getMethod().declaringClass();

            reflectiveClasses.produce(
                    ReflectiveClassBuildItem.builder(declaringClassName.toString()).methods(true).fields(true).build());

            String multiplexerClassName = declaringClassName + "_Multiplexer";
            reflectiveClasses
                    .produce(ReflectiveClassBuildItem.builder(multiplexerClassName).methods(true).fields(true).build());

            ClassCreator multiplexerClassCreator = ClassCreator.builder().classOutput(beanClassOutput)
                    .className(multiplexerClassName)
                    .superClass(declaringClassName.toString())
                    .build();

            multiplexerClassCreator.addAnnotation(Multiplexer.class);

            if (!BuiltinScope.isDeclaredOn(declaringClass)) {
                multiplexerClassCreator.addAnnotation(Singleton.class);
            }

            for (AnnotationInstance classAnnotation : declaringClass.declaredAnnotations()) {
                multiplexerClassCreator.addAnnotation(classAnnotation);
            }

            // Inject ErrorHandler
            FieldDescriptor errorHandlerFieldDescriptor = FieldDescriptor.of(multiplexerClassName, "errorHandler",
                    ErrorHandler.class);
            FieldCreator errorHandlerFieldCreator = multiplexerClassCreator.getFieldCreator(errorHandlerFieldDescriptor);
            errorHandlerFieldCreator.addAnnotation(Inject.class);
            errorHandlerFieldCreator.setModifiers(Modifier.PROTECTED);

            // Copy the constructors
            for (MethodInfo originalConstructor : declaringClass.constructors()) {
                MethodCreator constructorCreator = multiplexerClassCreator.getMethodCreator(MethodDescriptor.ofConstructor(
                        multiplexerClassName,
                        originalConstructor.parameterTypes().stream().map(t -> t.name().toString()).toArray(String[]::new)));

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
                for (short i = 0; i < originalConstructor.parameterTypes().size(); i++) {
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
                List<Type> originalMethodParameterTypes = originalMethod.parameterTypes();

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

                short j = 0;
                Map<Short, Short> parameterMapping = new HashMap<>();
                for (short i = 0; i < originalMethodParameterTypes.size(); i++) {
                    List<AnnotationInstance> originalMethodAnnotations = originalMethodParameterAnnotationMapping
                            .getOrDefault(i, Collections.emptyList());
                    if (originalMethodAnnotations.stream().anyMatch(ai -> CONFIG_FILE.equals(ai.name())) ||
                            GITHUB.equals(originalMethodParameterTypes.get(i).name()) ||
                            GITHUB_EVENT.equals(originalMethodParameterTypes.get(i).name()) ||
                            DYNAMIC_GRAPHQL_CLIENT.equals(originalMethodParameterTypes.get(i).name())) {
                        // if the parameter is annotated with @ConfigFile or is of type GitHub, GitHubEvent or DynamicGraphQLClient, we skip it
                        continue;
                    }

                    String parameterType;

                    if (i == payloadParameterPosition) {
                        parameterType = MultiplexedEvent.class.getName();
                    } else {
                        parameterType = originalMethodParameterTypes.get(i).name().toString();
                    }

                    parameterTypes.add(parameterType);
                    parameterMapping.put(i, j);
                    j++;
                }
                if (originalMethod.hasAnnotation(CONFIG_FILE)) {
                    parameterTypes.add(RequestScopeCachingGitHubConfigFileProvider.class.getName());
                }

                MethodCreator methodCreator = multiplexerClassCreator.getMethodCreator(
                        originalMethod.name() + "_" + HashUtil.sha1(eventSubscriberInstance.toString()),
                        originalMethod.returnType().name().toString(),
                        parameterTypes.toArray());

                for (Type exceptionType : originalMethod.exceptions()) {
                    methodCreator.addException(exceptionType.name().toString());
                }

                ResultHandle[] parameterValues = new ResultHandle[originalMethod.parameterTypes().size()];

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

                ResultHandle multiplexedEventRh = methodCreator.getMethodParam(parameterMapping.get(payloadParameterPosition));
                ResultHandle payloadRh = methodCreator.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(MultiplexedEvent.class, "getPayload", GHEventPayload.class),
                        multiplexedEventRh);

                TryBlock tryBlock = methodCreator.tryBlock();

                // generate the code of the method
                for (short originalMethodParameterIndex = 0; originalMethodParameterIndex < originalMethodParameterTypes
                        .size(); originalMethodParameterIndex++) {
                    List<AnnotationInstance> parameterAnnotations = originalMethodParameterAnnotationMapping.getOrDefault(
                            originalMethodParameterIndex,
                            Collections.emptyList());
                    Short multiplexerMethodParameterIndex = parameterMapping.get(originalMethodParameterIndex);
                    if (originalMethodParameterIndex == payloadParameterPosition) {
                        parameterValues[originalMethodParameterIndex] = payloadRh;
                    } else if (GITHUB.equals(originalMethodParameterTypes.get(originalMethodParameterIndex).name())) {
                        parameterValues[originalMethodParameterIndex] = tryBlock.invokeVirtualMethod(
                                MethodDescriptor.ofMethod(MultiplexedEvent.class, "getGitHub", GitHub.class),
                                tryBlock.getMethodParam(parameterMapping.get(payloadParameterPosition)));
                    } else if (GITHUB_EVENT.equals(originalMethodParameterTypes.get(originalMethodParameterIndex).name())) {
                        parameterValues[originalMethodParameterIndex] = tryBlock.invokeVirtualMethod(
                                MethodDescriptor.ofMethod(MultiplexedEvent.class, "getGitHubEvent", GitHubEvent.class),
                                tryBlock.getMethodParam(parameterMapping.get(payloadParameterPosition)));
                    } else if (DYNAMIC_GRAPHQL_CLIENT
                            .equals(originalMethodParameterTypes.get(originalMethodParameterIndex).name())) {
                        parameterValues[originalMethodParameterIndex] = tryBlock.invokeVirtualMethod(
                                MethodDescriptor.ofMethod(MultiplexedEvent.class, "getGitHubGraphQLClient",
                                        DynamicGraphQLClient.class),
                                tryBlock.getMethodParam(parameterMapping.get(payloadParameterPosition)));
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
                        ResultHandle configFileReaderRh = tryBlock.getMethodParam(parameterTypes.size() - 1);
                        ResultHandle ghRepositoryRh = tryBlock.invokeStaticMethod(MethodDescriptor
                                .ofMethod(PayloadHelper.class, "getRepository", GHRepository.class, GHEventPayload.class),
                                payloadRh);
                        ResultHandle configObject = tryBlock.invokeVirtualMethod(
                                MethodDescriptor.ofMethod(RequestScopeCachingGitHubConfigFileProvider.class, "getConfigObject",
                                        Object.class,
                                        GHRepository.class, String.class, ConfigFile.Source.class, Class.class),
                                configFileReaderRh,
                                ghRepositoryRh,
                                tryBlock.load(configFileAnnotationInstance.value().asString()),
                                tryBlock.load(ConfigFile.Source
                                        .valueOf(configFileAnnotationInstance.valueWithDefault(index, "source").asEnum())),
                                tryBlock.loadClass(configObjectType));
                        configObject = tryBlock.checkCast(configObject, configObjectType);

                        if (isOptional) {
                            configObject = tryBlock.invokeStaticMethod(
                                    MethodDescriptor.ofMethod(Optional.class, "ofNullable", Optional.class, Object.class),
                                    configObject);
                        }

                        parameterValues[originalMethodParameterIndex] = configObject;
                    } else {
                        parameterValues[originalMethodParameterIndex] = tryBlock
                                .getMethodParam(multiplexerMethodParameterIndex);
                    }
                }

                ResultHandle returnValue = tryBlock.invokeVirtualMethod(originalMethod, tryBlock.getThis(),
                        parameterValues);
                tryBlock.returnValue(returnValue);

                CatchBlockCreator catchBlock = tryBlock.addCatch(Throwable.class);
                catchBlock.invokeInterfaceMethod(
                        MethodDescriptor.ofMethod(ErrorHandler.class, "handleError", void.class, GitHubEvent.class,
                                GHEventPayload.class, Throwable.class),
                        catchBlock.readInstanceField(errorHandlerFieldDescriptor, catchBlock.getThis()),
                        catchBlock.invokeVirtualMethod(
                                MethodDescriptor.ofMethod(MultiplexedEvent.class, "getGitHubEvent", GitHubEvent.class),
                                multiplexedEventRh),
                        catchBlock.invokeVirtualMethod(
                                MethodDescriptor.ofMethod(MultiplexedEvent.class, "getPayload", GHEventPayload.class),
                                multiplexedEventRh),
                        catchBlock.getCaughtException());
                catchBlock.returnValue(null);
            }

            multiplexerClassCreator.close();
        }
    }

    private static ResultHandle fireAsyncAction(BytecodeCreator bytecodeCreator, LaunchMode launchMode, String className,
            ResultHandle gitHubEventRh, ResultHandle multiplexedEventRh, ResultHandle annotationLiteralArrayRh) {
        ResultHandle cdiEventRh = bytecodeCreator.invokeInterfaceMethod(EVENT_SELECT,
                bytecodeCreator.readInstanceField(
                        FieldDescriptor.of(className, EVENT_EMITTER_FIELD, Event.class),
                        bytecodeCreator.getThis()),
                annotationLiteralArrayRh);

        ResultHandle fireAsyncCompletionStageRH = bytecodeCreator.invokeInterfaceMethod(EVENT_FIRE_ASYNC, cdiEventRh,
                multiplexedEventRh);

        if (LaunchMode.TEST.equals(launchMode)) {
            ResultHandle toFutureRH = bytecodeCreator.invokeInterfaceMethod(COMPLETION_STAGE_TO_COMPLETABLE_FUTURE,
                    fireAsyncCompletionStageRH);
            return bytecodeCreator.invokeVirtualMethod(COMPLETABLE_FUTURE_JOIN, toFutureRH);
        } else {
            return fireAsyncCompletionStageRH;
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

    private static class RemoveBridgeMethodsClassVisitor extends ClassVisitor {

        private static final Logger LOG = Logger.getLogger(RemoveBridgeMethodsClassVisitor.class);

        private final String className;
        private final Set<String> methodsWithBridges;

        public RemoveBridgeMethodsClassVisitor(ClassVisitor visitor, String className, Set<String> methodsWithBridges) {
            super(Gizmo.ASM_API_VERSION, visitor);

            this.className = className;
            this.methodsWithBridges = methodsWithBridges;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            if (methodsWithBridges.contains(name) && ((access & Opcodes.ACC_BRIDGE) != 0)
                    && ((access & Opcodes.ACC_SYNTHETIC) != 0)) {

                if (LOG.isDebugEnabled()) {
                    LOG.debugf("Class %1$s - Removing method %2$s %3$s(%4$s)", className,
                            org.objectweb.asm.Type.getReturnType(descriptor), name,
                            Arrays.toString(org.objectweb.asm.Type.getArgumentTypes(descriptor)));
                }

                return null;
            }

            return super.visitMethod(access, name, descriptor, signature, exceptions);
        }
    }
}
