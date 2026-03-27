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
import static org.jboss.jandex.gizmo2.Jandex2Gizmo.classDescOf;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.lang.annotation.Annotation;
import java.lang.constant.ClassDesc;
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
import org.jboss.jandex.gizmo2.Jandex2Gizmo;
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
import io.quarkus.arc.deployment.GeneratedBeanGizmo2Adaptor;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.GeneratedClassGizmo2Adaptor;
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
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.gizmo2.ClassOutput;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.GenericType;
import io.quarkus.gizmo2.Gizmo;
import io.quarkus.gizmo2.LocalVar;
import io.quarkus.gizmo2.ParamVar;
import io.quarkus.gizmo2.TypeArgument;
import io.quarkus.gizmo2.creator.BlockCreator;
import io.quarkus.gizmo2.desc.ConstructorDesc;
import io.quarkus.gizmo2.desc.FieldDesc;
import io.quarkus.gizmo2.desc.MethodDesc;
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

    private static final MethodDesc EVENT_SELECT = MethodDesc.of(Event.class, "select", Event.class,
            Annotation[].class);
    private static final MethodDesc EVENT_FIRE_ASYNC = MethodDesc.of(Event.class, "fireAsync",
            CompletionStage.class, Object.class);
    private static final MethodDesc COMPLETION_STAGE_TO_COMPLETABLE_FUTURE = MethodDesc.of(
            CompletionStage.class,
            "toCompletableFuture", CompletableFuture.class);
    private static final MethodDesc COMPLETABLE_FUTURE_JOIN = MethodDesc.of(CompletableFuture.class,
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
            BuildProducer<GeneratedResourceBuildItem> generatedResources,
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

        ClassOutput classOutput = new GeneratedClassGizmo2Adaptor(generatedClasses, generatedResources, true);
        generateAnnotationLiterals(classOutput, dispatchingConfiguration);

        ClassOutput beanClassOutput = new GeneratedBeanGizmo2Adaptor(generatedBeans);
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
        Set<EventAnnotationLiteral> eventAnnotationLiterals = dispatchingConfiguration.getEventConfigurations().values()
                .stream()
                .map(EventDispatchingConfiguration::getEventAnnotationLiterals)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());

        Gizmo gizmo = Gizmo.create(classOutput).withDebugInfo(false).withParameters(false);

        for (EventAnnotationLiteral eventAnnotationLiteral : eventAnnotationLiterals) {
            String literalClassName = getLiteralClassName(eventAnnotationLiteral.getName());

            gizmo.class_(literalClassName, cc -> {
                cc.extends_(GenericType.ofClass(AnnotationLiteral.class,
                        TypeArgument.of(classDescOf(eventAnnotationLiteral.getName()))));
                cc.implements_(classDescOf(eventAnnotationLiteral.getName()));

                List<String> attributes = eventAnnotationLiteral.getAttributes();

                // Create instance fields for attributes and collect their descriptors
                List<FieldDesc> attributeFields = new ArrayList<>();
                for (String attribute : attributes) {
                    FieldDesc fieldDesc = cc.field(attribute, fc -> {
                        fc.setType(String.class);
                        fc.private_();
                    });
                    attributeFields.add(fieldDesc);
                }

                // Create constructor
                cc.constructor(constr -> {
                    constr.public_();
                    List<ParamVar> params = new ArrayList<>();
                    for (int i = 0; i < attributes.size(); i++) {
                        params.add(constr.parameter("param" + i, String.class));
                    }
                    constr.body(bc -> {
                        bc.invokeSpecial(ConstructorDesc.of(AnnotationLiteral.class), cc.this_());
                        for (int i = 0; i < attributes.size(); i++) {
                            bc.set(cc.this_().field(attributeFields.get(i)), params.get(i));
                        }
                        bc.return_();
                    });
                });

                // Create getter methods
                for (int i = 0; i < attributes.size(); i++) {
                    String attribute = attributes.get(i);
                    FieldDesc fieldDesc = attributeFields.get(i);
                    cc.method(attribute, mc -> {
                        mc.public_();
                        mc.returning(String.class);
                        mc.body(bc -> {
                            bc.return_(cc.this_().field(fieldDesc));
                        });
                    });
                }

                // If no attributes, create static array instance field with initializer
                if (attributes.isEmpty()) {
                    ClassDesc literalClassDesc = ClassDesc.of(literalClassName);
                    ClassDesc arrayType = literalClassDesc.arrayType();
                    cc.staticField(ARRAY_INSTANCE_FIELD_NAME, fc -> {
                        fc.setType(arrayType);
                        fc.public_();
                        fc.final_();
                        fc.setInitializer(bc -> {
                            // Create a single-element array with one instance of the annotation literal
                            List<Expr> arrayElements = new ArrayList<>();
                            arrayElements.add(bc.new_(ConstructorDesc.of(literalClassDesc)));
                            bc.yield(bc.newArray(literalClassDesc, arrayElements));
                        });
                    });
                }
            });
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

        Gizmo gizmo = Gizmo.create(beanClassOutput).withDebugInfo(false).withParameters(false);

        gizmo.class_(dispatcherClassName, cc -> {
            cc.addAnnotation(Singleton.class);
            cc.defaultConstructor();

            // Event field
            cc.field(EVENT_EMITTER_FIELD, fc -> {
                fc.setType(GenericType.ofClass(Event.class,
                        TypeArgument.of(MultiplexedEvent.class)));
                fc.addAnnotation(Inject.class);
                fc.protected_();
            });

            // GitHubService field
            cc.field(GITHUB_SERVICE_FIELD, fc -> {
                fc.setType(GitHubService.class);
                fc.addAnnotation(Inject.class);
                fc.protected_();
            });

            // dispatch method
            cc.method("dispatch", mc -> {
                mc.public_();
                mc.returning(void.class);
                ParamVar gitHubEventParam = mc.parameter("gitHubEvent", pc -> {
                    pc.setType(GitHubEvent.class);
                    pc.addAnnotation(ClassDesc.of(DotNames.OBSERVES.toString()),
                            java.lang.annotation.RetentionPolicy.RUNTIME, ab -> {
                            });
                });

                mc.body(b0 -> {
                    Expr thisExpr = mc.this_();
                    Expr gitHubEventExpr = gitHubEventParam;

                    LocalVar supportsInstallationVar = b0.localVar("supportsInstallation", boolean.class,
                            b0.invokeInterface(
                                    MethodDesc.of(GitHubEvent.class, "supportsInstallation", boolean.class),
                                    gitHubEventExpr));
                    LocalVar installationIdVar = b0.localVar("installationId", Long.class,
                            b0.invokeInterface(
                                    MethodDesc.of(GitHubEvent.class, "getInstallationId", Long.class),
                                    gitHubEventExpr));
                    LocalVar dispatchedEventVar = b0.localVar("dispatchedEvent", String.class,
                            b0.invokeInterface(
                                    MethodDesc.of(GitHubEvent.class, "getEvent", String.class),
                                    gitHubEventExpr));
                    LocalVar dispatchedActionVar = b0.localVar("dispatchedAction", String.class,
                            b0.invokeInterface(
                                    MethodDesc.of(GitHubEvent.class, "getAction", String.class),
                                    gitHubEventExpr));
                    LocalVar dispatchedPayloadVar = b0.localVar("dispatchedPayload", String.class,
                            b0.invokeInterface(
                                    MethodDesc.of(GitHubEvent.class, "getPayload", String.class),
                                    gitHubEventExpr));

                    b0.try_(tc -> {
                        tc.body(b1 -> {
                            // if the event supports installation, we can push the installation client
                            // if not, we have to use the very limited application client
                            LocalVar gitHubVar = b1.localVar("gitHub", GitHub.class, Const.ofNull(GitHub.class));
                            LocalVar gitHubGraphQLClientVar = b1.localVar("gitHubGraphQLClient", DynamicGraphQLClient.class,
                                    Const.ofNull(DynamicGraphQLClient.class));

                            b1.ifElse(supportsInstallationVar,
                                    b2 -> {
                                        // if the event supports installation
                                        if (dispatchingConfiguration.requiresGitHubClient()) {
                                            b2.set(gitHubVar, b2.invokeVirtual(
                                                    MethodDesc.of(GitHubService.class, "getInstallationClient", GitHub.class,
                                                            long.class),
                                                    thisExpr.field(
                                                            FieldDesc.of(ClassDesc.of(dispatcherClassName),
                                                                    GITHUB_SERVICE_FIELD,
                                                                    GitHubService.class)),
                                                    installationIdVar));
                                        } else {
                                            b2.set(gitHubVar, Const.ofNull(GitHub.class));
                                        }
                                        if (dispatchingConfiguration.requiresGraphQLClient()) {
                                            b2.set(gitHubGraphQLClientVar, b2.invokeVirtual(
                                                    MethodDesc.of(GitHubService.class, "getInstallationGraphQLClient",
                                                            DynamicGraphQLClient.class, long.class),
                                                    thisExpr.field(
                                                            FieldDesc.of(ClassDesc.of(dispatcherClassName),
                                                                    GITHUB_SERVICE_FIELD,
                                                                    GitHubService.class)),
                                                    installationIdVar));
                                        } else {
                                            b2.set(gitHubGraphQLClientVar, Const.ofNull(DynamicGraphQLClient.class));
                                        }
                                    },
                                    b2 -> {
                                        // if the event does not support installation
                                        if (dispatchingConfiguration.requiresGitHubClient()) {
                                            b2.set(gitHubVar, b2.invokeVirtual(
                                                    MethodDesc.of(GitHubService.class, "getTokenOrApplicationClient",
                                                            GitHub.class),
                                                    thisExpr.field(
                                                            FieldDesc.of(ClassDesc.of(dispatcherClassName),
                                                                    GITHUB_SERVICE_FIELD,
                                                                    GitHubService.class))));
                                        } else {
                                            b2.set(gitHubVar, Const.ofNull(GitHub.class));
                                        }
                                        if (dispatchingConfiguration.requiresGraphQLClient()) {
                                            b2.set(gitHubGraphQLClientVar, b2.invokeVirtual(
                                                    MethodDesc.of(GitHubService.class, "getTokenGraphQLClientOrNull",
                                                            DynamicGraphQLClient.class),
                                                    thisExpr.field(
                                                            FieldDesc.of(ClassDesc.of(dispatcherClassName),
                                                                    GITHUB_SERVICE_FIELD,
                                                                    GitHubService.class))));
                                        } else {
                                            b2.set(gitHubGraphQLClientVar, Const.ofNull(DynamicGraphQLClient.class));
                                        }
                                    });

                            for (EventDispatchingConfiguration eventDispatchingConfiguration : dispatchingConfiguration
                                    .getEventConfigurations()
                                    .values()) {
                                Expr eventExpr = Const.of(eventDispatchingConfiguration.getEvent());
                                String payloadType = eventDispatchingConfiguration.getPayloadType();

                                if (Events.ALL.equals(eventDispatchingConfiguration.getEvent())) {
                                    // Process all events
                                    processEventDispatching(b1, dispatcherClassName, launchMode.getLaunchMode(),
                                            gitHubEventExpr, dispatchedEventVar, dispatchedActionVar, dispatchedPayloadVar,
                                            gitHubVar, gitHubGraphQLClientVar, eventDispatchingConfiguration, payloadType,
                                            thisExpr);
                                } else {
                                    // Process event only if it matches
                                    b1.if_(b1.invokeVirtual(MethodDesc.of(Object.class, "equals", boolean.class, Object.class),
                                            eventExpr, dispatchedEventVar),
                                            b2 -> processEventDispatching(b2, dispatcherClassName, launchMode.getLaunchMode(),
                                                    gitHubEventExpr, dispatchedEventVar, dispatchedActionVar,
                                                    dispatchedPayloadVar,
                                                    gitHubVar, gitHubGraphQLClientVar, eventDispatchingConfiguration,
                                                    payloadType, thisExpr));
                                }
                            }
                        });

                        tc.catch_(Throwable.class, "t", (b1, caughtException) -> {
                            b1.invokeVirtual(
                                    MethodDesc.of(ErrorHandlerBridgeFunction.class, "apply", Void.class, GitHubEvent.class,
                                            Throwable.class),
                                    b1.getStaticField(FieldDesc.of(ErrorHandlerBridgeFunction.class, "INSTANCE")),
                                    gitHubEventExpr,
                                    caughtException);
                        });
                    });

                    b0.return_();
                });
            });
        });
    }

    private static void processEventDispatching(BlockCreator bc, String dispatcherClassName, LaunchMode launchMode,
            Expr gitHubEventExpr, LocalVar dispatchedEventVar, LocalVar dispatchedActionVar, LocalVar dispatchedPayloadVar,
            LocalVar gitHubVar, LocalVar gitHubGraphQLClientVar,
            EventDispatchingConfiguration eventDispatchingConfiguration, String payloadType,
            Expr thisExpr) {

        LocalVar payloadInstanceVar;
        if (payloadType != null) {
            payloadInstanceVar = bc.localVar("payloadInstance", GHEventPayload.class,
                    bc.invokeVirtual(
                            MethodDesc.of(GitHub.class, "parseEventPayload", GHEventPayload.class, Reader.class, Class.class),
                            gitHubVar,
                            bc.new_(ConstructorDesc.of(StringReader.class, String.class), dispatchedPayloadVar),
                            Const.of(ClassDesc.of(payloadType))));
        } else {
            // all events are raw, no need to actually parse the payload
            payloadInstanceVar = bc.localVar("payloadInstance", GHEventPayload.class, Const.ofNull(GHEventPayload.class));
        }

        LocalVar multiplexedEventVar = bc.localVar("multiplexedEvent", MultiplexedEvent.class,
                bc.new_(
                        ConstructorDesc.of(MultiplexedEvent.class, GitHubEvent.class, GHEventPayload.class, GitHub.class,
                                DynamicGraphQLClient.class),
                        gitHubEventExpr, payloadInstanceVar, gitHubVar, gitHubGraphQLClientVar));

        for (Entry<String, Set<EventAnnotation>> eventAnnotationsEntry : eventDispatchingConfiguration.getEventAnnotations()
                .entrySet()) {
            String action = eventAnnotationsEntry.getKey();

            for (EventAnnotation eventAnnotation : eventAnnotationsEntry.getValue()) {
                LocalVar annotationLiteralArrayVar;
                String literalClassName = getLiteralClassName(eventAnnotation.getName());

                if (eventAnnotation.getValues().isEmpty()) {
                    annotationLiteralArrayVar = bc.localVar("annotationLiteralArray", Annotation[].class,
                            bc.getStaticField(
                                    FieldDesc.of(ClassDesc.of(literalClassName), ARRAY_INSTANCE_FIELD_NAME,
                                            ClassDesc.of(literalClassName).arrayType())));
                } else {
                    List<Expr> literalParameters = new ArrayList<>(eventAnnotation.getValues().size());
                    for (AnnotationValue eventAnnotationValue : eventAnnotation.getValues()) {
                        literalParameters.add(Const.of(eventAnnotationValue.asString()));
                    }

                    ClassDesc stringClassDesc = ClassDesc.of(String.class.getName());
                    ClassDesc[] paramTypes = Collections.nCopies(eventAnnotation.getValues().size(), stringClassDesc)
                            .toArray(new ClassDesc[0]);
                    Expr annotationLiteralExpr = bc.new_(
                            ConstructorDesc.of(ClassDesc.of(literalClassName), paramTypes),
                            literalParameters.toArray(new Expr[0]));
                    List<Expr> arrayElements = new ArrayList<>();
                    arrayElements.add(annotationLiteralExpr);
                    annotationLiteralArrayVar = bc.localVar("annotationLiteralArray", Annotation[].class,
                            bc.newArray(Annotation.class, arrayElements));
                }

                if (Actions.ALL.equals(action)) {
                    fireAsyncAction(bc, launchMode, dispatcherClassName, gitHubEventExpr, multiplexedEventVar,
                            annotationLiteralArrayVar, thisExpr);
                } else {
                    bc.if_(bc.invokeVirtual(MethodDesc.of(Object.class, "equals", boolean.class, Object.class),
                            Const.of(action), dispatchedActionVar),
                            b2 -> fireAsyncAction(b2, launchMode, dispatcherClassName, gitHubEventExpr, multiplexedEventVar,
                                    annotationLiteralArrayVar, thisExpr));
                }
            }
        }
    }

    private static Expr fireAsyncAction(BlockCreator bc, LaunchMode launchMode, String className,
            Expr gitHubEventExpr, Expr multiplexedEventVar, Expr annotationLiteralArrayVar, Expr thisExpr) {
        Expr cdiEventExpr = bc.invokeInterface(EVENT_SELECT,
                thisExpr.field(FieldDesc.of(ClassDesc.of(className), EVENT_EMITTER_FIELD, Event.class)),
                annotationLiteralArrayVar);

        Expr fireAsyncCompletionStageExpr = bc.invokeInterface(EVENT_FIRE_ASYNC, cdiEventExpr, multiplexedEventVar);

        if (LaunchMode.TEST.equals(launchMode)) {
            Expr toFutureExpr = bc.invokeInterface(COMPLETION_STAGE_TO_COMPLETABLE_FUTURE, fireAsyncCompletionStageExpr);
            return bc.invokeVirtual(COMPLETABLE_FUTURE_JOIN, toFutureExpr);
        } else {
            return fireAsyncCompletionStageExpr;
        }
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

            Gizmo gizmo = Gizmo.create(beanClassOutput).withDebugInfo(false).withParameters(false);

            gizmo.class_(multiplexerClassName, cc -> {
                cc.extends_(classDescOf(declaringClassName));
                cc.addAnnotation(Multiplexer.class);

                if (!BuiltinScope.isDeclaredOn(declaringClass)) {
                    cc.addAnnotation(Singleton.class);
                }

                for (AnnotationInstance classAnnotation : declaringClass.declaredAnnotations()) {
                    Jandex2Gizmo.addAnnotation(cc, classAnnotation, index);
                }

                // OpenTelemetry integration
                final FieldDesc telemetryTracesReporterFieldDescriptor;
                if (openTelemetryTracesIntegrationEnabled) {
                    telemetryTracesReporterFieldDescriptor = cc.field("telemetryTracesReporter", fc -> {
                        fc.setType(TelemetryTracesReporter.class);
                        fc.addAnnotation(Inject.class);
                        fc.protected_();
                    });
                } else {
                    // won't be used, as the consumer code is protected by the same condition
                    telemetryTracesReporterFieldDescriptor = null;
                }
                final FieldDesc telemetryMetricsReporterFieldDescriptor;
                if (openTelemetryMetricsIntegrationEnabled) {
                    telemetryMetricsReporterFieldDescriptor = cc.field("telemetryMetricsReporter", fc -> {
                        fc.setType(TelemetryMetricsReporter.class);
                        fc.addAnnotation(Inject.class);
                        fc.protected_();
                    });
                } else {
                    // won't be used, as the consumer code is protected by the same condition
                    telemetryMetricsReporterFieldDescriptor = null;
                }

                // Inject ErrorHandler
                FieldDesc errorHandlerFieldDescriptor = cc.field("errorHandler", fc -> {
                    fc.setType(ErrorHandler.class);
                    fc.addAnnotation(Inject.class);
                    fc.protected_();
                });

                // Copy the constructors
                for (MethodInfo originalConstructor : declaringClass.constructors()) {
                    List<AnnotationInstance> originalMethodAnnotations = originalConstructor.annotations().stream()
                            .filter(ai -> ai.target().kind() == Kind.METHOD).toList();
                    Map<Short, List<AnnotationInstance>> originalConstructorParameterAnnotationMapping = originalConstructor
                            .annotations().stream()
                            .filter(ai -> ai.target().kind() == Kind.METHOD_PARAMETER)
                            .collect(Collectors.groupingBy(ai -> ai.target().asMethodParameter().position()));

                    cc.constructor(constr -> {
                        // Add method-level annotations
                        for (AnnotationInstance originalMethodAnnotation : originalMethodAnnotations) {
                            Jandex2Gizmo.addAnnotation(constr, originalMethodAnnotation, index);
                        }

                        // Create parameters with annotations
                        List<ParamVar> constructorParamsVar = new ArrayList<>();
                        for (short i = 0; i < originalConstructor.parameterTypes().size(); i++) {
                            final short paramIndex = i;
                            List<AnnotationInstance> originalConstructorParameterAnnotations = originalConstructorParameterAnnotationMapping
                                    .getOrDefault(paramIndex, Collections.emptyList());

                            ParamVar param = constr.parameter("param" + i, pc -> {
                                pc.setType(classDescOf(originalConstructor.parameterTypes().get(paramIndex).name()));

                                // Add parameter annotations
                                for (AnnotationInstance originalConstructorParameterAnnotation : originalConstructorParameterAnnotations) {
                                    Jandex2Gizmo.addAnnotation(pc, originalConstructorParameterAnnotation, index);
                                }
                            });
                            constructorParamsVar.add(param);
                        }

                        constr.body(bc -> {
                            bc.invokeSpecial(
                                    ConstructorDesc.of(classDescOf(originalConstructor.declaringClass().name()),
                                            originalConstructor.parameterTypes().stream()
                                                    .map(t -> classDescOf(t.name()))
                                                    .toArray(ClassDesc[]::new)),
                                    cc.this_(),
                                    constructorParamsVar.toArray(new Expr[0]));
                            bc.return_();
                        });
                    });
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
                    if (originalMethod.hasAnnotation(DotNames.OBSERVES)
                            || originalMethod.hasAnnotation(DotNames.OBSERVES_ASYNC)) {
                        LOG.warn(
                                "Methods listening to GitHub events may not be annotated with @Observes or @ObservesAsync. Offending method: "
                                        + originalMethod.declaringClass().name() + "#" + originalMethod);
                    }

                    List<ClassDesc> parameterTypes = new ArrayList<>();
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
                                (GITHUB_EVENT.equals(originalMethodParameterTypes.get(i).name())
                                        && i != payloadParameterPosition)
                                ||
                                DYNAMIC_GRAPHQL_CLIENT.equals(originalMethodParameterTypes.get(i).name())) {
                            // if the parameter is annotated with @ConfigFile or is of type GitHub, GitHubEvent or DynamicGraphQLClient, we skip it
                            continue;
                        }

                        ClassDesc parameterType;

                        if (i == payloadParameterPosition) {
                            parameterType = ClassDesc.of(MultiplexedEvent.class.getName());
                        } else {
                            parameterType = classDescOf(originalMethodParameterTypes.get(i).name());
                        }

                        parameterTypes.add(parameterType);
                        parameterMapping.put(i, j);
                        j++;
                    }
                    if (originalMethod.hasAnnotation(CONFIG_FILE)) {
                        parameterTypes.add(ClassDesc.of(RequestScopeCachingGitHubConfigFileProvider.class.getName()));
                    }

                    final short finalPayloadParameterPosition = payloadParameterPosition;
                    final boolean finalIsPayloadGitHubEvent = isPayloadGitHubEvent;

                    cc.method(originalMethod.name() + "_" + HashUtil.sha1(eventSubscriberInstance.toString()), mc -> {
                        mc.returning(classDescOf(originalMethod.returnType().name()));

                        // Add exceptions
                        for (Type exceptionType : originalMethod.exceptions()) {
                            mc.throws_(classDescOf(exceptionType.name()));
                        }

                        // Create parameters and store them
                        List<ParamVar> methodParams = new ArrayList<>();
                        for (short i = 0; i < parameterTypes.size(); i++) {
                            final short paramIndex = i;
                            ParamVar param = mc.parameter("param" + i, pc -> {
                                pc.setType(parameterTypes.get(paramIndex));

                                // Find the original parameter index for this generated parameter
                                for (Map.Entry<Short, Short> mappingEntry : parameterMapping.entrySet()) {
                                    if (mappingEntry.getValue() == paramIndex) {
                                        short origParamIndex = mappingEntry.getKey();
                                        List<AnnotationInstance> parameterAnnotations = originalMethodParameterAnnotationMapping
                                                .getOrDefault(origParamIndex, Collections.emptyList());

                                        if (!parameterAnnotations.isEmpty()) {
                                            if (parameterAnnotations.stream()
                                                    .anyMatch(ai -> ai.name().equals(eventSubscriberInstance.name()))) {
                                                pc.addAnnotation(ClassDesc.of(DotNames.OBSERVES_ASYNC.toString()),
                                                        java.lang.annotation.RetentionPolicy.RUNTIME, ab -> {
                                                        });
                                                Jandex2Gizmo.addAnnotation(pc, eventSubscriberInstance, index);
                                            } else {
                                                for (AnnotationInstance annotationInstance : parameterAnnotations) {
                                                    Jandex2Gizmo.addAnnotation(pc, annotationInstance, index);
                                                }
                                            }
                                        }
                                        break;
                                    }
                                }
                            });
                            methodParams.add(param);
                        }

                        mc.body(b0 -> {
                            Expr thisExpr = mc.this_();
                            Expr multiplexedEventExpr = methodParams.get(parameterMapping.get(finalPayloadParameterPosition));

                            // Store gitHubEvent and payload in locals for use across nested blocks
                            LocalVar gitHubEventVar = b0.localVar("gitHubEventVar", GitHubEvent.class,
                                    b0.invokeVirtual(
                                            MethodDesc.of(MultiplexedEvent.class, "getGitHubEvent", GitHubEvent.class),
                                            multiplexedEventExpr));
                            LocalVar payloadVar;
                            if (!finalIsPayloadGitHubEvent) {
                                payloadVar = b0.localVar("payloadVar", GHEventPayload.class,
                                        b0.invokeVirtual(
                                                MethodDesc.of(MultiplexedEvent.class, "getPayload", GHEventPayload.class),
                                                multiplexedEventExpr));
                            } else {
                                payloadVar = b0.localVar("payloadVar", GHEventPayload.class,
                                        Const.ofNull(GHEventPayload.class));
                            }

                            // OpenTelemetry integration
                            final LocalVar telemetrySpanWrapperVar;
                            final LocalVar telemetryScopeWrapperVar;
                            if (openTelemetryTracesIntegrationEnabled) {
                                telemetrySpanWrapperVar = b0.localVar("telemetrySpanWrapper", TelemetrySpanWrapper.class,
                                        b0.invokeInterface(
                                                MethodDesc.of(TelemetryTracesReporter.class,
                                                        "createGitHubEventListeningMethodSpan",
                                                        TelemetrySpanWrapper.class, GitHubEvent.class, String.class,
                                                        String.class,
                                                        String.class),
                                                thisExpr.field(telemetryTracesReporterFieldDescriptor),
                                                gitHubEventVar, Const.of(declaringClassName.toString()),
                                                Const.of(originalMethod.name()), Const.of(originalMethod.toString())));

                                telemetryScopeWrapperVar = b0.localVar("telemetryScopeWrapper", TelemetryScopeWrapper.class,
                                        b0.invokeInterface(
                                                MethodDesc.of(TelemetryTracesReporter.class, "makeCurrent",
                                                        TelemetryScopeWrapper.class,
                                                        TelemetrySpanWrapper.class),
                                                thisExpr.field(telemetryTracesReporterFieldDescriptor),
                                                telemetrySpanWrapperVar));
                            } else {
                                // won't be used, as the consumer code is protected by the same condition
                                telemetrySpanWrapperVar = null;
                                telemetryScopeWrapperVar = null;
                            }

                            b0.try_(tc -> {
                                tc.body(b1 -> {
                                    // Build parameter values for the original method call
                                    Expr[] parameterValues = new Expr[originalMethod.parameterTypes().size()];

                                    for (short originalMethodParameterIndex = 0; originalMethodParameterIndex < originalMethodParameterTypes
                                            .size(); originalMethodParameterIndex++) {
                                        List<AnnotationInstance> parameterAnnotations = originalMethodParameterAnnotationMapping
                                                .getOrDefault(
                                                        originalMethodParameterIndex,
                                                        Collections.emptyList());
                                        Short multiplexerMethodParameterIndex = parameterMapping
                                                .get(originalMethodParameterIndex);

                                        if (originalMethodParameterIndex == finalPayloadParameterPosition
                                                && !finalIsPayloadGitHubEvent) {
                                            parameterValues[originalMethodParameterIndex] = b1.cast(payloadVar,
                                                    classDescOf(originalMethodParameterTypes
                                                            .get(originalMethodParameterIndex).name()));
                                        } else if (GITHUB
                                                .equals(originalMethodParameterTypes.get(originalMethodParameterIndex)
                                                        .name())) {
                                            parameterValues[originalMethodParameterIndex] = b1.invokeVirtual(
                                                    MethodDesc.of(MultiplexedEvent.class, "getGitHub", GitHub.class),
                                                    multiplexedEventExpr);
                                        } else if (GITHUB_EVENT.equals(
                                                originalMethodParameterTypes.get(originalMethodParameterIndex).name())) {
                                            parameterValues[originalMethodParameterIndex] = gitHubEventVar;
                                        } else if (DYNAMIC_GRAPHQL_CLIENT
                                                .equals(originalMethodParameterTypes.get(originalMethodParameterIndex)
                                                        .name())) {
                                            parameterValues[originalMethodParameterIndex] = b1.invokeVirtual(
                                                    MethodDesc.of(MultiplexedEvent.class, "getGitHubGraphQLClient",
                                                            DynamicGraphQLClient.class),
                                                    multiplexedEventExpr);
                                        } else if (parameterAnnotations.stream()
                                                .anyMatch(ai -> ai.name().equals(CONFIG_FILE))) {
                                            AnnotationInstance configFileAnnotationInstance = parameterAnnotations.stream()
                                                    .filter(ai -> ai.name().equals(CONFIG_FILE)).findFirst().get();
                                            String configObjectType = originalMethodParameterTypes
                                                    .get(originalMethodParameterIndex).name()
                                                    .toString();

                                            boolean isOptional = false;
                                            if (Optional.class.getName().equals(configObjectType)) {
                                                if (originalMethodParameterTypes.get(originalMethodParameterIndex)
                                                        .kind() != Type.Kind.PARAMETERIZED_TYPE) {
                                                    throw new IllegalStateException(
                                                            "Optional is used but not parameterized for method " +
                                                                    originalMethod.declaringClass().name() + "#"
                                                                    + originalMethod);
                                                }
                                                isOptional = true;
                                                configObjectType = originalMethodParameterTypes
                                                        .get(originalMethodParameterIndex)
                                                        .asParameterizedType().arguments().get(0)
                                                        .name().toString();
                                            }

                                            // it's a config file, we will use the ConfigFileReader (last parameter of the method) and inject the result
                                            Expr configFileReaderExpr = methodParams.get(parameterTypes.size() - 1);
                                            Expr ghRepositoryExpr;
                                            if (!finalIsPayloadGitHubEvent) {
                                                ghRepositoryExpr = b1.invokeStatic(
                                                        MethodDesc.of(PayloadHelper.class, "getRepository", GHRepository.class,
                                                                GHEventPayload.class),
                                                        payloadVar);
                                            } else {
                                                ghRepositoryExpr = b1.invokeStatic(
                                                        MethodDesc.of(GitHub.class, "getRepository", GHRepository.class,
                                                                String.class),
                                                        b1.invokeInterface(
                                                                MethodDesc.of(GitHubEvent.class, "getRepositoryOrThrow",
                                                                        String.class),
                                                                gitHubEventVar));
                                            }

                                            Expr configObjectExpr = b1.invokeVirtual(
                                                    MethodDesc.of(RequestScopeCachingGitHubConfigFileProvider.class,
                                                            "getConfigObject",
                                                            Object.class,
                                                            GHRepository.class, String.class, ConfigFile.Source.class,
                                                            Class.class),
                                                    configFileReaderExpr,
                                                    ghRepositoryExpr,
                                                    Const.of(configFileAnnotationInstance.value().asString()),
                                                    Const.of(ConfigFile.Source.valueOf(
                                                            configFileAnnotationInstance.valueWithDefault(index, "source")
                                                                    .asEnum())),
                                                    Const.of(ClassDesc.of(configObjectType)));
                                            configObjectExpr = b1.cast(configObjectExpr, ClassDesc.of(configObjectType));

                                            if (isOptional) {
                                                configObjectExpr = b1.invokeStatic(
                                                        MethodDesc.of(Optional.class, "ofNullable", Optional.class,
                                                                Object.class),
                                                        configObjectExpr);
                                            }

                                            parameterValues[originalMethodParameterIndex] = configObjectExpr;
                                        } else {
                                            parameterValues[originalMethodParameterIndex] = methodParams
                                                    .get(multiplexerMethodParameterIndex);
                                        }
                                    }

                                    // Invoke the original method
                                    ClassDesc originalMethodOwner = classDescOf(originalMethod.declaringClass().name());
                                    ClassDesc originalMethodReturnType = classDescOf(originalMethod.returnType().name());
                                    ClassDesc[] originalMethodParamTypes = originalMethod.parameterTypes().stream()
                                            .map(t -> classDescOf(t.name()))
                                            .toArray(ClassDesc[]::new);
                                    java.lang.constant.MethodTypeDesc originalMethodTypeDesc = java.lang.constant.MethodTypeDesc
                                            .of(originalMethodReturnType, originalMethodParamTypes);
                                    MethodDesc originalMethodDesc = io.quarkus.gizmo2.desc.ClassMethodDesc.of(
                                            originalMethodOwner,
                                            originalMethod.name(),
                                            originalMethodTypeDesc);
                                    Expr returnValue = b1.invokeVirtual(originalMethodDesc, thisExpr, parameterValues);

                                    // OpenTelemetry integration - success path
                                    if (openTelemetryTracesIntegrationEnabled) {
                                        b1.invokeInterface(
                                                MethodDesc.of(TelemetryTracesReporter.class, "reportSuccess", void.class,
                                                        GitHubEvent.class, TelemetrySpanWrapper.class),
                                                thisExpr.field(telemetryTracesReporterFieldDescriptor),
                                                gitHubEventVar, telemetrySpanWrapperVar);
                                        // we don't have a finally clause in Gizmo 2 that works with catch returns,
                                        // so we duplicate the close/endSpan in both the try and catch blocks
                                        b1.invokeInterface(MethodDesc.of(AutoCloseable.class, "close", void.class),
                                                telemetryScopeWrapperVar);
                                        b1.invokeInterface(
                                                MethodDesc.of(TelemetryTracesReporter.class, "endSpan", void.class,
                                                        TelemetrySpanWrapper.class),
                                                thisExpr.field(telemetryTracesReporterFieldDescriptor),
                                                telemetrySpanWrapperVar);
                                    }
                                    if (openTelemetryMetricsIntegrationEnabled) {
                                        b1.invokeInterface(
                                                MethodDesc.of(TelemetryMetricsReporter.class,
                                                        "incrementGitHubEventMethodSuccess",
                                                        void.class,
                                                        GitHubEvent.class, String.class, String.class, String.class),
                                                thisExpr.field(telemetryMetricsReporterFieldDescriptor),
                                                gitHubEventVar, Const.of(declaringClassName.toString()),
                                                Const.of(originalMethod.name()), Const.of(originalMethod.toString()));
                                    }

                                    if (originalMethod.returnType().kind() == Type.Kind.VOID) {
                                        b1.return_();
                                    } else {
                                        b1.return_(returnValue);
                                    }
                                });

                                tc.catch_(Throwable.class, "t", (b1, caughtException) -> {
                                    b1.invokeInterface(
                                            MethodDesc.of(ErrorHandler.class, "handleError", void.class, GitHubEvent.class,
                                                    GHEventPayload.class, Throwable.class),
                                            thisExpr.field(errorHandlerFieldDescriptor),
                                            gitHubEventVar,
                                            payloadVar,
                                            caughtException);

                                    // OpenTelemetry integration - exception path
                                    if (openTelemetryTracesIntegrationEnabled) {
                                        b1.invokeInterface(
                                                MethodDesc.of(TelemetryTracesReporter.class, "reportException", void.class,
                                                        GitHubEvent.class,
                                                        TelemetrySpanWrapper.class, Throwable.class),
                                                thisExpr.field(telemetryTracesReporterFieldDescriptor),
                                                gitHubEventVar, telemetrySpanWrapperVar, caughtException);
                                        // close/endSpan duplicated here since finally doesn't run on catch return
                                        b1.invokeInterface(MethodDesc.of(AutoCloseable.class, "close", void.class),
                                                telemetryScopeWrapperVar);
                                        b1.invokeInterface(
                                                MethodDesc.of(TelemetryTracesReporter.class, "endSpan", void.class,
                                                        TelemetrySpanWrapper.class),
                                                thisExpr.field(telemetryTracesReporterFieldDescriptor),
                                                telemetrySpanWrapperVar);
                                    }
                                    if (openTelemetryMetricsIntegrationEnabled) {
                                        b1.invokeInterface(
                                                MethodDesc.of(TelemetryMetricsReporter.class,
                                                        "incrementGitHubEventMethodError",
                                                        void.class,
                                                        GitHubEvent.class, String.class, String.class, String.class,
                                                        Throwable.class),
                                                thisExpr.field(telemetryMetricsReporterFieldDescriptor),
                                                gitHubEventVar, Const.of(declaringClassName.toString()),
                                                Const.of(originalMethod.name()), Const.of(originalMethod.toString()),
                                                caughtException);
                                    }

                                    if (originalMethod.returnType().kind() == org.jboss.jandex.Type.Kind.VOID) {
                                        b1.return_();
                                    } else {
                                        b1.return_(Const.ofNull(classDescOf(originalMethod.returnType().name())));
                                    }
                                });
                            });
                        });
                    });
                }

            }); // Close cc lambda
        } // Close for loop
    }

    private static String getLiteralClassName(DotName annotationName) {
        return annotationName + "_AnnotationLiteral";
    }

    private static class RemoveBridgeMethodsClassVisitor extends ClassVisitor {

        private static final Logger LOG = Logger.getLogger(RemoveBridgeMethodsClassVisitor.class);

        private final String className;
        private final Set<String> methodsWithBridges;

        public RemoveBridgeMethodsClassVisitor(ClassVisitor visitor, String className, Set<String> methodsWithBridges) {
            super(Opcodes.ASM9, visitor);

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
