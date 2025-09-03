package io.quarkiverse.githubapp.deployment;

import static io.quarkiverse.githubapp.deployment.GitHubAppDotNames.CONFIG_FILE;
import static io.quarkiverse.githubapp.deployment.GitHubAppDotNames.DYNAMIC_GRAPHQL_CLIENT;
import static io.quarkiverse.githubapp.deployment.GitHubAppDotNames.ERROR_HANDLER;
import static io.quarkiverse.githubapp.deployment.GitHubAppDotNames.EVENT;
import static io.quarkiverse.githubapp.deployment.GitHubAppDotNames.GITHUB;
import static io.quarkiverse.githubapp.deployment.GitHubAppDotNames.GITHUB_EVENT;
import static io.quarkiverse.githubapp.deployment.GitHubAppDotNames.JAVA_HTTP_CLIENT_TELEMETRY;
import static io.quarkiverse.githubapp.deployment.GitHubAppDotNames.OPENTELEMETRY_JAVA_HTTP_CLIENT_FACTORY;
import static io.quarkiverse.githubapp.deployment.GitHubAppDotNames.OPENTELEMETRY_METRICS_REPORTER;
import static io.quarkiverse.githubapp.deployment.GitHubAppDotNames.OPENTELEMETRY_TRACES_REPORTER;
import static io.quarkiverse.githubapp.deployment.GitHubAppDotNames.RAW_EVENT;
import static io.quarkus.gizmo.Type.classType;
import static io.quarkus.gizmo.Type.parameterizedType;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;

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
import io.quarkiverse.githubapp.TokenGitHubClients;
import io.quarkiverse.githubapp.deployment.DispatchingConfiguration.EventAnnotation;
import io.quarkiverse.githubapp.deployment.DispatchingConfiguration.EventAnnotationLiteral;
import io.quarkiverse.githubapp.deployment.DispatchingConfiguration.EventDispatchingConfiguration;
import io.quarkiverse.githubapp.deployment.DispatchingConfiguration.EventDispatchingMethod;
import io.quarkiverse.githubapp.error.ErrorHandler;
import io.quarkiverse.githubapp.event.Actions;
import io.quarkiverse.githubapp.event.Events;
import io.quarkiverse.githubapp.runtime.GitHubAppRecorder;
import io.quarkiverse.githubapp.runtime.MultiplexedEvent;
import io.quarkiverse.githubapp.runtime.Multiplexer;
import io.quarkiverse.githubapp.runtime.RequestScopeCachingGitHubConfigFileProvider;
import io.quarkiverse.githubapp.runtime.Routes;
import io.quarkiverse.githubapp.runtime.UtilsProducer;
import io.quarkiverse.githubapp.runtime.config.CheckedConfigProvider;
import io.quarkiverse.githubapp.runtime.error.DefaultErrorHandler;
import io.quarkiverse.githubapp.runtime.error.ErrorHandlerBridgeFunction;
import io.quarkiverse.githubapp.runtime.github.DefaultJavaHttpClientFactory;
import io.quarkiverse.githubapp.runtime.github.GitHubConfigFileProviderImpl;
import io.quarkiverse.githubapp.runtime.github.GitHubFileDownloader;
import io.quarkiverse.githubapp.runtime.github.GitHubService;
import io.quarkiverse.githubapp.runtime.github.PayloadHelper;
import io.quarkiverse.githubapp.runtime.replay.ReplayEvent;
import io.quarkiverse.githubapp.runtime.replay.ReplayEventsRoute;
import io.quarkiverse.githubapp.runtime.signing.JwtTokenCreator;
import io.quarkiverse.githubapp.runtime.signing.PayloadSignatureChecker;
import io.quarkiverse.githubapp.runtime.smee.SmeeIoForwarder;
import io.quarkiverse.githubapp.runtime.telemetry.noop.NoopTelemetryMetricsReporter;
import io.quarkiverse.githubapp.runtime.telemetry.noop.NoopTelemetryTracesReporter;
import io.quarkiverse.githubapp.telemetry.TelemetryMetricsReporter;
import io.quarkiverse.githubapp.telemetry.TelemetryScopeWrapper;
import io.quarkiverse.githubapp.telemetry.TelemetrySpanWrapper;
import io.quarkiverse.githubapp.telemetry.TelemetryTracesReporter;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.arc.processor.MethodDescriptors;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
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
import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BranchResult;
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
    private static final String ARRAY_INSTANCE_FIELD_NAME = "ARRAY_INSTANCE";

    private static final DotName WITH_BRIDGE_METHODS = DotName
            .createSimple("com.infradna.tool.bridge_method_injector.WithBridgeMethods");

    private static final GACT QUARKIVERSE_GITHUB_APP_GACT = new GACT("io.quarkiverse.githubapp",
            "quarkus-github-app-ui", null, "jar");
    private static final String REPLAY_UI_RESOURCES_PREFIX = "META-INF/resources/replay-ui/";
    private static final String REPLAY_UI_PATH = "replay";

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
            reflectiveHierarchies.produce(ReflectiveHierarchyBuildItem.builder(parameterType)
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
    void additionalBeans(CombinedIndexBuildItem index, Capabilities capabilities,
            GitHubAppBuildTimeConfig gitHubAppBuildTimeConfig,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<GitHubAppOpenTelemetryTracesIntegrationEnabledBuildItem> openTelemetryTracesIntegrationEnabled,
            BuildProducer<GitHubAppOpenTelemetryMetricsIntegrationEnabledBuildItem> openTelemetryMetricsIntegrationEnabled) {
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
                CheckedConfigProvider.class,
                TokenGitHubClients.class)
                .setUnremovable();

        for (ClassInfo errorHandler : index.getIndex().getAllKnownImplementations(ERROR_HANDLER)) {
            additionalBeanBuildItemBuilder.addBeanClass(errorHandler.name().toString());
        }

        if (capabilities.isPresent(Capability.OPENTELEMETRY_TRACER)
                && gitHubAppBuildTimeConfig.telemetry().tracesEnabled().orElse(true)) {
            openTelemetryTracesIntegrationEnabled.produce(new GitHubAppOpenTelemetryTracesIntegrationEnabledBuildItem());
            additionalBeanBuildItemBuilder.addBeanClass(OPENTELEMETRY_TRACES_REPORTER.toString());
            if (QuarkusClassLoader.isClassPresentAtRuntime(JAVA_HTTP_CLIENT_TELEMETRY.toString())) {
                additionalBeanBuildItemBuilder.addBeanClass(OPENTELEMETRY_JAVA_HTTP_CLIENT_FACTORY.toString());
            } else {
                additionalBeanBuildItemBuilder.addBeanClass(DefaultJavaHttpClientFactory.class);
            }
        } else {
            additionalBeanBuildItemBuilder.addBeanClass(NoopTelemetryTracesReporter.class);
            additionalBeanBuildItemBuilder.addBeanClass(DefaultJavaHttpClientFactory.class);
        }

        if (capabilities.isPresent(Capability.OPENTELEMETRY_METRICS)
                && gitHubAppBuildTimeConfig.telemetry().metricsEnabled().orElse(true)) {
            openTelemetryMetricsIntegrationEnabled.produce(new GitHubAppOpenTelemetryMetricsIntegrationEnabledBuildItem());
            additionalBeanBuildItemBuilder.addBeanClass(OPENTELEMETRY_METRICS_REPORTER.toString());
        } else {
            additionalBeanBuildItemBuilder.addBeanClass(NoopTelemetryMetricsReporter.class);
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
     * and more specifically with Byte Buddy (see <a href="https://github.com/raphw/byte-buddy/issues/1162">ByteBuddy issue
     * #1162</a>.
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
            Optional<GitHubAppOpenTelemetryTracesIntegrationEnabledBuildItem> openTelemetryTracesIntegrationEnabled,
            Optional<GitHubAppOpenTelemetryMetricsIntegrationEnabledBuildItem> openTelemetryMetricsIntegrationEnabled,
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
                        allEventDefinitions.stream().map(EventDefinition::getAnnotation).collect(Collectors.toSet()))));

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
        generateMultiplexers(beanClassOutput, index, dispatchingConfiguration,
                openTelemetryTracesIntegrationEnabled.isPresent(), openTelemetryMetricsIntegrationEnabled.isPresent(),
                reflectiveClasses);
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
            BuildProducer<RouteBuildItem> routes) throws IOException {
        if (launchMode.getLaunchMode() != LaunchMode.DEVELOPMENT) {
            return;
        }

        WebJarResultsBuildItem.WebJarResult webJarResult = webJarResults.byArtifactKey(QUARKIVERSE_GITHUB_APP_GACT);
        if (webJarResult == null) {
            return;
        }

        String replayUiPath = httpRootPath.resolvePath(REPLAY_UI_PATH);

        Handler<RoutingContext> handler = recorder.replayUiHandler(webJarResult.getFinalDestination(), replayUiPath,
                webJarResult.getWebRootConfigurations(), shutdownContext);
        routes.produce(httpRootPath.routeBuilder()
                .route(REPLAY_UI_PATH)
                .handler(handler)
                .displayOnNotFoundPage("Replay UI")
                .build());
        routes.produce(httpRootPath.routeBuilder().route(REPLAY_UI_PATH + "/*").handler(handler).build());

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
                    .toList();
            for (AnnotationInstance eventSubscriberInstance : eventSubscriberInstances) {
                String action = eventDefinition.getAction() != null ? eventDefinition.getAction()
                        : (eventSubscriberInstance.value() != null ? eventSubscriberInstance.value().asString() : Actions.ALL);

                MethodParameterInfo annotatedParameter = eventSubscriberInstance.target().asMethodParameter();
                MethodInfo methodInfo = annotatedParameter.method();
                DotName annotatedParameterType = annotatedParameter.method().parameterTypes().get(annotatedParameter.position())
                        .name();
                if (!eventDefinition.getPayloadType().equals(annotatedParameterType)
                        && !GITHUB_EVENT.equals(annotatedParameterType)) {
                    throw new IllegalStateException(
                            "Parameter subscribing to a GitHub '" + eventDefinition.getEvent()
                                    + "' event must be of type '" + eventDefinition.getPayloadType()
                                    + "' or '" + GITHUB_EVENT + "'. Offending method: " + methodInfo.declaringClass().name()
                                    + "#" + methodInfo);
                }

                configuration
                        .getOrCreateEventConfiguration(eventDefinition.getEvent(), eventDefinition.getPayloadType().toString())
                        .addEventAnnotation(action, eventSubscriberInstance, eventSubscriberInstance.valuesWithDefaults(index));
                configuration.addEventDispatchingMethod(new EventDispatchingMethod(eventSubscriberInstance, methodInfo));
            }
        }

        // Handle raw events
        Collection<AnnotationInstance> rawEventSubscriberInstances = index.getAnnotations(RAW_EVENT)
                .stream()
                .filter(ai -> ai.target().kind() == Kind.METHOD_PARAMETER)
                .filter(ai -> !Modifier.isInterface(ai.target().asMethodParameter().method().declaringClass().flags()))
                .toList();
        for (AnnotationInstance rawEventSubscriberInstance : rawEventSubscriberInstances) {
            String event = rawEventSubscriberInstance.valueWithDefault(index, "event").asString();
            String action = rawEventSubscriberInstance.valueWithDefault(index, "action").asString();

            MethodParameterInfo annotatedParameter = rawEventSubscriberInstance.target().asMethodParameter();
            MethodInfo methodInfo = annotatedParameter.method();
            DotName annotatedParameterType = annotatedParameter.method().parameterTypes().get(annotatedParameter.position())
                    .name();
            if (!GITHUB_EVENT.equals(annotatedParameterType)) {
                throw new IllegalStateException(
                        "Parameter subscribing to a GitHub "
                                + "raw event must be of type '" + GITHUB_EVENT
                                + "'. Offending method: " + methodInfo.declaringClass().name() + "#" + methodInfo);
            }

            configuration
                    .getOrCreateEventConfiguration(event, null)
                    .addEventAnnotation(action, rawEventSubscriberInstance,
                            rawEventSubscriberInstance.valuesWithDefaults(index));
            configuration.addEventDispatchingMethod(new EventDispatchingMethod(rawEventSubscriberInstance, methodInfo));
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

                if (eventAnnotationLiteral.getAttributes().isEmpty()) {
                    FieldCreator arrayInstanceFieldCreator = literalClassCreator.getFieldCreator(ARRAY_INSTANCE_FIELD_NAME,
                            "[L" + literalClassName + ";");
                    arrayInstanceFieldCreator.setModifiers(ACC_PUBLIC | ACC_STATIC | ACC_FINAL);
                    MethodCreator clinit = literalClassCreator.getMethodCreator("<clinit>", void.class);
                    clinit.setModifiers(ACC_STATIC);
                    ResultHandle singletonInstance = clinit.newArray(literalClassName, 1);
                    clinit.writeArrayValue(singletonInstance, clinit.load(0),
                            clinit.newInstance(constructorCreator.getMethodDescriptor()));
                    clinit.writeStaticField(arrayInstanceFieldCreator.getFieldDescriptor(), singletonInstance);
                    clinit.returnVoid();
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

        ResultHandle installationIdRh = dispatchMethodCreator.invokeInterfaceMethod(
                MethodDescriptor.ofMethod(GitHubEvent.class, "getInstallationId", Long.class),
                gitHubEventRh);
        ResultHandle dispatchedEventRh = dispatchMethodCreator.invokeInterfaceMethod(
                MethodDescriptor.ofMethod(GitHubEvent.class, "getEvent", String.class),
                gitHubEventRh);
        ResultHandle dispatchedActionRh = dispatchMethodCreator.invokeInterfaceMethod(
                MethodDescriptor.ofMethod(GitHubEvent.class, "getAction", String.class),
                gitHubEventRh);
        ResultHandle dispatchedPayloadRh = dispatchMethodCreator.invokeInterfaceMethod(
                MethodDescriptor.ofMethod(GitHubEvent.class, "getPayload", String.class),
                gitHubEventRh);

        TryBlock tryBlock = dispatchMethodCreator.tryBlock();

        // if the installation id is defined, we can push the installation client
        // if not, we have to use the very limited application client
        AssignableResultHandle gitHubRh = tryBlock.createVariable(GitHub.class);
        AssignableResultHandle gitHubGraphQLClientRh = tryBlock.createVariable(DynamicGraphQLClient.class);
        BranchResult testInstallationId = tryBlock.ifNotNull(installationIdRh);
        BytecodeCreator installationIdSet = testInstallationId.trueBranch();
        installationIdSet.assign(gitHubRh, installationIdSet.invokeVirtualMethod(
                MethodDescriptor.ofMethod(GitHubService.class, "getInstallationClient", GitHub.class, long.class),
                installationIdSet.readInstanceField(
                        FieldDescriptor.of(dispatcherClassCreator.getClassName(), GITHUB_SERVICE_FIELD, GitHubService.class),
                        installationIdSet.getThis()),
                installationIdRh));
        if (dispatchingConfiguration.requiresGraphQLClient()) {
            installationIdSet.assign(gitHubGraphQLClientRh, installationIdSet.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(GitHubService.class, "getInstallationGraphQLClient", DynamicGraphQLClient.class,
                            long.class),
                    installationIdSet.readInstanceField(
                            FieldDescriptor.of(dispatcherClassCreator.getClassName(), GITHUB_SERVICE_FIELD,
                                    GitHubService.class),
                            installationIdSet.getThis()),
                    installationIdRh));
        } else {
            installationIdSet.assign(gitHubGraphQLClientRh, installationIdSet.loadNull());
        }
        BytecodeCreator installationIdNull = testInstallationId.falseBranch();
        installationIdNull.assign(gitHubRh, installationIdNull.invokeVirtualMethod(
                MethodDescriptor.ofMethod(GitHubService.class, "getTokenOrApplicationClient", GitHub.class),
                installationIdNull.readInstanceField(
                        FieldDescriptor.of(dispatcherClassCreator.getClassName(), GITHUB_SERVICE_FIELD, GitHubService.class),
                        installationIdNull.getThis())));
        if (dispatchingConfiguration.requiresGraphQLClient()) {
            installationIdNull.assign(gitHubGraphQLClientRh, installationIdNull.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(GitHubService.class, "getTokenGraphQLClientOrNull", DynamicGraphQLClient.class),
                    installationIdNull.readInstanceField(
                            FieldDescriptor.of(dispatcherClassCreator.getClassName(), GITHUB_SERVICE_FIELD,
                                    GitHubService.class),
                            installationIdNull.getThis())));
        } else {
            installationIdNull.assign(gitHubGraphQLClientRh, installationIdNull.loadNull());
        }

        for (EventDispatchingConfiguration eventDispatchingConfiguration : dispatchingConfiguration.getEventConfigurations()
                .values()) {
            ResultHandle eventRh = tryBlock.load(eventDispatchingConfiguration.getEvent());
            String payloadType = eventDispatchingConfiguration.getPayloadType();

            BytecodeCreator eventMatchesCreator;

            if (Events.ALL.equals(eventDispatchingConfiguration.getEvent())) {
                eventMatchesCreator = tryBlock;
            } else {
                eventMatchesCreator = tryBlock
                        .ifTrue(tryBlock.invokeVirtualMethod(MethodDescriptors.OBJECT_EQUALS, eventRh,
                                dispatchedEventRh))
                        .trueBranch();
            }

            ResultHandle payloadInstanceRh;
            if (payloadType != null) {
                payloadInstanceRh = eventMatchesCreator.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(GitHub.class, "parseEventPayload", GHEventPayload.class, Reader.class,
                                Class.class),
                        gitHubRh,
                        eventMatchesCreator.newInstance(MethodDescriptor.ofConstructor(StringReader.class, String.class),
                                dispatchedPayloadRh),
                        eventMatchesCreator.loadClass(payloadType));
            } else {
                // all events are raw, no need to actually parse the payload
                payloadInstanceRh = eventMatchesCreator.loadNull();
            }

            ResultHandle multiplexedEventRh = eventMatchesCreator.newInstance(MethodDescriptor
                    .ofConstructor(MultiplexedEvent.class, GitHubEvent.class, GHEventPayload.class, GitHub.class,
                            DynamicGraphQLClient.class),
                    gitHubEventRh, payloadInstanceRh, gitHubRh, gitHubGraphQLClientRh);

            for (Entry<String, Set<EventAnnotation>> eventAnnotationsEntry : eventDispatchingConfiguration.getEventAnnotations()
                    .entrySet()) {
                String action = eventAnnotationsEntry.getKey();

                for (EventAnnotation eventAnnotation : eventAnnotationsEntry.getValue()) {
                    ResultHandle annotationLiteralArrayRh;
                    String literalClassName = getLiteralClassName(eventAnnotation.getName());

                    if (eventAnnotation.getValues().isEmpty()) {
                        annotationLiteralArrayRh = eventMatchesCreator
                                .readStaticField(
                                        FieldDescriptor.of(literalClassName, ARRAY_INSTANCE_FIELD_NAME,
                                                "[L" + literalClassName + ";"));
                    } else {
                        Class<?>[] literalParameterTypes = new Class<?>[eventAnnotation.getValues().size()];
                        Arrays.fill(literalParameterTypes, String.class);
                        List<ResultHandle> literalParameters = new ArrayList<>(eventAnnotation.getValues().size());
                        for (AnnotationValue eventAnnotationValue : eventAnnotation.getValues()) {
                            literalParameters.add(eventMatchesCreator.load(eventAnnotationValue.asString()));
                        }

                        ResultHandle annotationLiteralRh = eventMatchesCreator.newInstance(MethodDescriptor
                                .ofConstructor(literalClassName, (Object[]) literalParameterTypes),
                                literalParameters.toArray(ResultHandle[]::new));
                        annotationLiteralArrayRh = eventMatchesCreator.newArray(Annotation.class, 1);
                        eventMatchesCreator.writeArrayValue(annotationLiteralArrayRh, 0, annotationLiteralRh);
                    }

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
        }

        CatchBlockCreator catchBlockCreator = tryBlock.addCatch(Throwable.class);
        catchBlockCreator.invokeVirtualMethod(
                MethodDescriptor.ofMethod(ErrorHandlerBridgeFunction.class, "apply", Void.class, GitHubEvent.class,
                        Throwable.class),
                catchBlockCreator.readStaticField(
                        FieldDescriptor.of(ErrorHandlerBridgeFunction.class, "INSTANCE", ErrorHandlerBridgeFunction.class)),
                gitHubEventRh,
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
            boolean openTelemetryTracesIntegrationEnabled,
            boolean openTelemetryMetricsIntegrationEnabled,
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

            // OpenTelemetry integration
            final FieldDescriptor telemetryTracesReporterFieldDescriptor;
            if (openTelemetryTracesIntegrationEnabled) {
                telemetryTracesReporterFieldDescriptor = FieldDescriptor.of(multiplexerClassName, "telemetryTracesReporter",
                        TelemetryTracesReporter.class);
                FieldCreator telemetryTracesReporterFieldCreator = multiplexerClassCreator
                        .getFieldCreator(telemetryTracesReporterFieldDescriptor);
                telemetryTracesReporterFieldCreator.addAnnotation(Inject.class);
                telemetryTracesReporterFieldCreator.setModifiers(Modifier.PROTECTED);
            } else {
                // won't be used, as the consumer code is protected by the same condition
                telemetryTracesReporterFieldDescriptor = null;
            }
            final FieldDescriptor telemetryMetricsReporterFieldDescriptor;
            if (openTelemetryMetricsIntegrationEnabled) {
                telemetryMetricsReporterFieldDescriptor = FieldDescriptor.of(multiplexerClassName, "telemetryMetricsReporter",
                        TelemetryMetricsReporter.class);
                FieldCreator telemetryMetricsReporterFieldCreator = multiplexerClassCreator
                        .getFieldCreator(telemetryMetricsReporterFieldDescriptor);
                telemetryMetricsReporterFieldCreator.addAnnotation(Inject.class);
                telemetryMetricsReporterFieldCreator.setModifiers(Modifier.PROTECTED);
            } else {
                // won't be used, as the consumer code is protected by the same condition
                telemetryMetricsReporterFieldDescriptor = null;
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
                        .filter(ai -> ai.target().kind() == Kind.METHOD).toList();
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
                short payloadParameterPosition = -1;
                boolean isPayloadGitHubEvent = false;
                for (short i = 0; i < originalMethodParameterTypes.size(); i++) {
                    List<AnnotationInstance> parameterAnnotations = originalMethodParameterAnnotationMapping.getOrDefault(i,
                            Collections.emptyList());
                    if (parameterAnnotations.stream().anyMatch(ai -> ai.name().equals(eventSubscriberInstance.name()))) {
                        payloadParameterPosition = i;
                        isPayloadGitHubEvent = GITHUB_EVENT
                                .equals(originalMethodParameterTypes.get(payloadParameterPosition).name());
                        break;
                    }
                }

                if (payloadParameterPosition == -1) {
                    throw new IllegalStateException("Unable to find the payload parameter position. Offending method: "
                            + originalMethod.declaringClass().name() + "#" + originalMethod);
                }

                short j = 0;
                Map<Short, Short> parameterMapping = new HashMap<>();
                for (short i = 0; i < originalMethodParameterTypes.size(); i++) {
                    List<AnnotationInstance> originalMethodAnnotations = originalMethodParameterAnnotationMapping
                            .getOrDefault(i, Collections.emptyList());
                    if (originalMethodAnnotations.stream().anyMatch(ai -> CONFIG_FILE.equals(ai.name())) ||
                            GITHUB.equals(originalMethodParameterTypes.get(i).name()) ||
                            (GITHUB_EVENT.equals(originalMethodParameterTypes.get(i).name()) && i != payloadParameterPosition)
                            ||
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
                ResultHandle gitHubEventRh = methodCreator.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(MultiplexedEvent.class, "getGitHubEvent", GitHubEvent.class),
                        multiplexedEventRh);
                ResultHandle payloadRh;
                if (!isPayloadGitHubEvent) {
                    payloadRh = methodCreator.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(MultiplexedEvent.class, "getPayload", GHEventPayload.class),
                            multiplexedEventRh);
                } else {
                    payloadRh = methodCreator.loadNull();
                }

                final ResultHandle telemetrySpanWrapperRh;
                final ResultHandle telemetryScopeWrapperRh;
                if (openTelemetryTracesIntegrationEnabled) {
                    telemetrySpanWrapperRh = methodCreator.invokeInterfaceMethod(
                            MethodDescriptor.ofMethod(TelemetryTracesReporter.class, "createGitHubEventListeningMethodSpan",
                                    TelemetrySpanWrapper.class, GitHubEvent.class, String.class, String.class, String.class),
                            methodCreator.readInstanceField(telemetryTracesReporterFieldDescriptor, methodCreator.getThis()),
                            gitHubEventRh, methodCreator.load(declaringClassName.toString()),
                            methodCreator.load(originalMethod.name()), methodCreator.load(originalMethod.toString()));
                    telemetryScopeWrapperRh = methodCreator.invokeInterfaceMethod(
                            MethodDescriptor.ofMethod(TelemetryTracesReporter.class, "makeCurrent", TelemetryScopeWrapper.class,
                                    TelemetrySpanWrapper.class),
                            methodCreator.readInstanceField(telemetryTracesReporterFieldDescriptor, methodCreator.getThis()),
                            telemetrySpanWrapperRh);
                } else {
                    // won't be used, as the consumer code is protected by the same condition
                    telemetrySpanWrapperRh = null;
                    telemetryScopeWrapperRh = null;
                }

                TryBlock tryBlock = methodCreator.tryBlock();

                // generate the code of the method
                for (short originalMethodParameterIndex = 0; originalMethodParameterIndex < originalMethodParameterTypes
                        .size(); originalMethodParameterIndex++) {
                    List<AnnotationInstance> parameterAnnotations = originalMethodParameterAnnotationMapping.getOrDefault(
                            originalMethodParameterIndex,
                            Collections.emptyList());
                    Short multiplexerMethodParameterIndex = parameterMapping.get(originalMethodParameterIndex);
                    if (originalMethodParameterIndex == payloadParameterPosition && !isPayloadGitHubEvent) {
                        parameterValues[originalMethodParameterIndex] = payloadRh;
                    } else if (GITHUB.equals(originalMethodParameterTypes.get(originalMethodParameterIndex).name())) {
                        parameterValues[originalMethodParameterIndex] = tryBlock.invokeVirtualMethod(
                                MethodDescriptor.ofMethod(MultiplexedEvent.class, "getGitHub", GitHub.class),
                                multiplexedEventRh);
                    } else if (GITHUB_EVENT.equals(originalMethodParameterTypes.get(originalMethodParameterIndex).name())) {
                        parameterValues[originalMethodParameterIndex] = gitHubEventRh;
                    } else if (DYNAMIC_GRAPHQL_CLIENT
                            .equals(originalMethodParameterTypes.get(originalMethodParameterIndex).name())) {
                        parameterValues[originalMethodParameterIndex] = tryBlock.invokeVirtualMethod(
                                MethodDescriptor.ofMethod(MultiplexedEvent.class, "getGitHubGraphQLClient",
                                        DynamicGraphQLClient.class),
                                multiplexedEventRh);
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
                        ResultHandle ghRepositoryRh;
                        if (!isPayloadGitHubEvent) {
                            ghRepositoryRh = tryBlock.invokeStaticMethod(MethodDescriptor
                                    .ofMethod(PayloadHelper.class, "getRepository", GHRepository.class, GHEventPayload.class),
                                    payloadRh);
                        } else {
                            ghRepositoryRh = tryBlock.invokeStaticMethod(MethodDescriptor
                                    .ofMethod(GitHub.class, "getRepository", GHRepository.class, String.class),
                                    tryBlock.invokeInterfaceMethod(
                                            MethodDescriptor.ofMethod(GitHubEvent.class, "getRepositoryOrThrow", String.class),
                                            gitHubEventRh));
                        }
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
                // OpenTelemetry integration
                if (openTelemetryTracesIntegrationEnabled) {
                    tryBlock.invokeInterfaceMethod(
                            MethodDescriptor.ofMethod(TelemetryTracesReporter.class, "reportSuccess", void.class,
                                    GitHubEvent.class, TelemetrySpanWrapper.class),
                            tryBlock.readInstanceField(telemetryTracesReporterFieldDescriptor, tryBlock.getThis()),
                            gitHubEventRh, telemetrySpanWrapperRh);
                    // we don't have a finally clause in Gizmo 1, so we copy this clause in both the try and the catch...
                    tryBlock.invokeInterfaceMethod(MethodDescriptor.ofMethod(AutoCloseable.class, "close", void.class),
                            telemetryScopeWrapperRh);
                    tryBlock.invokeInterfaceMethod(
                            MethodDescriptor.ofMethod(TelemetryTracesReporter.class, "endSpan", void.class,
                                    TelemetrySpanWrapper.class),
                            tryBlock.readInstanceField(telemetryTracesReporterFieldDescriptor, tryBlock.getThis()),
                            telemetrySpanWrapperRh);
                }
                if (openTelemetryMetricsIntegrationEnabled) {
                    tryBlock.invokeInterfaceMethod(
                            MethodDescriptor.ofMethod(TelemetryMetricsReporter.class, "incrementGitHubEventMethodSuccess",
                                    void.class,
                                    GitHubEvent.class, String.class, String.class, String.class),
                            tryBlock.readInstanceField(telemetryMetricsReporterFieldDescriptor, tryBlock.getThis()),
                            gitHubEventRh, tryBlock.load(declaringClassName.toString()),
                            tryBlock.load(originalMethod.name()), tryBlock.load(originalMethod.toString()));
                }
                tryBlock.returnValue(returnValue);

                CatchBlockCreator catchBlock = tryBlock.addCatch(Throwable.class);
                catchBlock.invokeInterfaceMethod(
                        MethodDescriptor.ofMethod(ErrorHandler.class, "handleError", void.class, GitHubEvent.class,
                                GHEventPayload.class, Throwable.class),
                        catchBlock.readInstanceField(errorHandlerFieldDescriptor, catchBlock.getThis()),
                        gitHubEventRh,
                        payloadRh,
                        catchBlock.getCaughtException());
                // OpenTelemetry integration
                if (openTelemetryTracesIntegrationEnabled) {
                    catchBlock.invokeInterfaceMethod(
                            MethodDescriptor.ofMethod(TelemetryTracesReporter.class, "reportException", void.class,
                                    GitHubEvent.class,
                                    TelemetrySpanWrapper.class, Throwable.class),
                            catchBlock.readInstanceField(telemetryTracesReporterFieldDescriptor, catchBlock.getThis()),
                            gitHubEventRh, telemetrySpanWrapperRh, catchBlock.getCaughtException());
                    // we don't have a finally clause in Gizmo 1, so we copy this clause in both the try and the catch...
                    catchBlock.invokeInterfaceMethod(MethodDescriptor.ofMethod(AutoCloseable.class, "close", void.class),
                            telemetryScopeWrapperRh);
                    catchBlock.invokeInterfaceMethod(
                            MethodDescriptor.ofMethod(TelemetryTracesReporter.class, "endSpan", void.class,
                                    TelemetrySpanWrapper.class),
                            catchBlock.readInstanceField(telemetryTracesReporterFieldDescriptor, catchBlock.getThis()),
                            telemetrySpanWrapperRh);
                }
                if (openTelemetryMetricsIntegrationEnabled) {
                    catchBlock.invokeInterfaceMethod(
                            MethodDescriptor.ofMethod(TelemetryMetricsReporter.class, "incrementGitHubEventMethodError",
                                    void.class,
                                    GitHubEvent.class, String.class, String.class, String.class, Throwable.class),
                            catchBlock.readInstanceField(telemetryMetricsReporterFieldDescriptor, catchBlock.getThis()),
                            gitHubEventRh, catchBlock.load(declaringClassName.toString()),
                            catchBlock.load(originalMethod.name()), catchBlock.load(originalMethod.toString()),
                            catchBlock.getCaughtException());
                }
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
