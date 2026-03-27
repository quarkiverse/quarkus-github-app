package io.quarkiverse.githubapp.command.airline.deployment;

import static io.quarkiverse.githubapp.command.airline.deployment.GitHubAppCommandAirlineDotNames.ALIAS;
import static io.quarkiverse.githubapp.command.airline.deployment.GitHubAppCommandAirlineDotNames.CLI;
import static io.quarkiverse.githubapp.command.airline.deployment.GitHubAppCommandAirlineDotNames.CLI_OPTIONS;
import static io.quarkiverse.githubapp.command.airline.deployment.GitHubAppCommandAirlineDotNames.COMMAND;
import static io.quarkiverse.githubapp.command.airline.deployment.GitHubAppCommandAirlineDotNames.COMMAND_OPTIONS;
import static io.quarkiverse.githubapp.command.airline.deployment.GitHubAppCommandAirlineDotNames.DEPENDENT;
import static io.quarkiverse.githubapp.command.airline.deployment.GitHubAppCommandAirlineDotNames.GH_EVENT_PAYLOAD_ISSUE_COMMENT;
import static io.quarkiverse.githubapp.command.airline.deployment.GitHubAppCommandAirlineDotNames.PERMISSION;
import static io.quarkiverse.githubapp.command.airline.deployment.GitHubAppCommandAirlineDotNames.TEAM;
import static org.jboss.jandex.gizmo2.Jandex2Gizmo.classDescOf;

import java.lang.constant.ClassDesc;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.jandex.gizmo2.Jandex2Gizmo;
import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHPermissionType;
import org.kohsuke.github.GHReaction;
import org.kohsuke.github.ReactionContent;

import io.quarkiverse.githubapp.GitHubEvent;
import io.quarkiverse.githubapp.command.airline.CliOptions.ParseErrorStrategy;
import io.quarkiverse.githubapp.command.airline.CommandOptions.CommandScope;
import io.quarkiverse.githubapp.command.airline.CommandOptions.ExecutionErrorStrategy;
import io.quarkiverse.githubapp.command.airline.CommandOptions.ReactionStrategy;
import io.quarkiverse.githubapp.command.airline.ExecutionErrorHandler;
import io.quarkiverse.githubapp.command.airline.ParseErrorHandler;
import io.quarkiverse.githubapp.command.airline.runtime.AbstractCommandDispatcher;
import io.quarkiverse.githubapp.command.airline.runtime.AbstractCommandDispatcher.CommandExecutionContext;
import io.quarkiverse.githubapp.command.airline.runtime.CliConfig;
import io.quarkiverse.githubapp.command.airline.runtime.CommandConfig;
import io.quarkiverse.githubapp.command.airline.runtime.CommandExecutionException;
import io.quarkiverse.githubapp.command.airline.runtime.CommandPermissionConfig;
import io.quarkiverse.githubapp.command.airline.runtime.CommandTeamConfig;
import io.quarkiverse.githubapp.command.airline.runtime.DefaultExecutionErrorHandler;
import io.quarkiverse.githubapp.command.airline.runtime.DefaultParseErrorHandler;
import io.quarkiverse.githubapp.command.airline.runtime.util.Reactions;
import io.quarkiverse.githubapp.deployment.AdditionalEventDispatchingClassesIndexBuildItem;
import io.quarkiverse.githubapp.deployment.GitHubAppDotNames;
import io.quarkiverse.githubapp.event.IssueComment;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmo2Adaptor;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.gizmo2.ClassOutput;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.Gizmo;
import io.quarkus.gizmo2.LocalVar;
import io.quarkus.gizmo2.ParamVar;
import io.quarkus.gizmo2.creator.BlockCreator;
import io.quarkus.gizmo2.desc.ConstructorDesc;
import io.quarkus.gizmo2.desc.MethodDesc;

class GitHubAppCommandAirlineProcessor {

    private static final String FEATURE = "github-app-command-airline";
    private static final String RUN_METHOD = "run";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    public void beanConfig(CombinedIndexBuildItem index,
            BuildProducer<BeanDefiningAnnotationBuildItem> beanDefiningAnnotations,
            BuildProducer<AnnotationsTransformerBuildItem> annotationsTransformer,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans) {
        beanDefiningAnnotations.produce(new BeanDefiningAnnotationBuildItem(COMMAND, DEPENDENT));

        annotationsTransformer
                .produce(new AnnotationsTransformerBuildItem(new HideAirlineInjectAnnotationsTransformer(index.getIndex())));

        // default error handlers
        additionalBeans
                .produce(new AdditionalBeanBuildItem(DefaultExecutionErrorHandler.class, DefaultParseErrorHandler.class));
        unremovableBeans.produce(
                UnremovableBeanBuildItem.beanTypes(ExecutionErrorHandler.class, ParseErrorHandler.class));
    }

    @BuildStep
    public void indexAnnotations(BuildProducer<AdditionalIndexedClassesBuildItem> additionalIndexedClasses) {
        // adding the *Options annotations
        additionalIndexedClasses
                .produce(new AdditionalIndexedClassesBuildItem(CLI_OPTIONS.toString(), COMMAND_OPTIONS.toString()));

        // adding Runnable as it's a likely candidate for simple commands
        additionalIndexedClasses
                .produce(new AdditionalIndexedClassesBuildItem(Runnable.class.getName()));
    }

    @BuildStep
    public void generate(CombinedIndexBuildItem index,
            BuildProducer<AdditionalEventDispatchingClassesIndexBuildItem> additionalEventDispatchingClassesIndexes,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            BuildProducer<GeneratedBeanBuildItem> generatedBeans) {
        validate(index.getIndex());

        IndexedGeneratedBeansBuildProducer indexedGeneratedBeans = new IndexedGeneratedBeansBuildProducer(
                generatedBeans);
        ClassOutput classOutput = new GeneratedBeanGizmo2Adaptor(indexedGeneratedBeans);

        for (AnnotationInstance cliAnnotationInstance : index.getIndex().getAnnotations(CLI)) {
            List<String> aliases = getAliases(cliAnnotationInstance);

            Map<DotName, ClassInfo> allCommands = getAllCommands(index.getIndex(), cliAnnotationInstance);
            final String[] forReflection = allCommands.values().stream().map(ClassInfo::toString).toArray(String[]::new);
            reflectiveClasses
                    .produce(ReflectiveClassBuildItem.builder(forReflection).constructors().methods().fields().build());

            generateCommandDispatcher(index.getIndex(), classOutput, cliAnnotationInstance, aliases,
                    findCommonInterfaceWithRunMethod(index.getIndex(), aliases.get(0), allCommands.values()),
                    allCommands.values());
        }

        if (!indexedGeneratedBeans.isEmpty()) {
            additionalEventDispatchingClassesIndexes
                    .produce(new AdditionalEventDispatchingClassesIndexBuildItem(indexedGeneratedBeans.getIndex()));
        }
    }

    private static void validate(IndexView index) {
        Set<String> nonStaticNestedCommandClasses = index.getAnnotations(COMMAND).stream()
                .filter(ai -> ai.target().kind() == Kind.CLASS)
                .map(ai -> ai.target().asClass())
                .filter(ci -> ci.enclosingClass() != null && !Modifier.isStatic(ci.flags()))
                .map(ci -> ci.name().toString())
                .collect(Collectors.toCollection(TreeSet::new));

        if (!nonStaticNestedCommandClasses.isEmpty()) {
            throw new IllegalStateException("Nested classes marked with @Command must be made static. Offending classes: "
                    + String.join(", ", nonStaticNestedCommandClasses));
        }
    }

    private static void generateCommandDispatcher(IndexView index, ClassOutput classOutput,
            AnnotationInstance cliAnnotationInstance, List<String> aliases,
            RunMethod runMethod, Collection<ClassInfo> allCommands) {
        ClassInfo cliClassInfo = cliAnnotationInstance.target().asClass();
        String commandDispatcherClassName = cliClassInfo.name() + "CommandDispatcherImpl";

        Gizmo gizmo = Gizmo.create(classOutput).withDebugInfo(false).withParameters(false);

        gizmo.class_(commandDispatcherClassName, cc -> {
            cc.extends_(AbstractCommandDispatcher.class);

            generateCommandDispatcherConstructor(cc, commandDispatcherClassName, index, cliClassInfo, aliases);
            generateCommandDispatcherGetCommandConfigsMethod(cc, index, allCommands);
            generateCommandDispatcherGetCommandPermissionConfigsMethod(cc, allCommands);
            generateCommandDispatcherGetCommandTeamConfigsMethod(cc, allCommands);
            generateCommandDispatcherDispatchMethod(cc, index, runMethod);
        });
    }

    private static void generateCommandDispatcherDispatchMethod(io.quarkus.gizmo2.creator.ClassCreator cc,
            IndexView index, RunMethod runMethod) {
        List<Type> originalMethodParameterTypes = runMethod.getMethod().parameterTypes();
        Map<Short, List<AnnotationInstance>> originalMethodParameterAnnotationMapping = runMethod.getMethod().annotations()
                .stream()
                .filter(ai -> ai.target().kind() == Kind.METHOD_PARAMETER)
                .collect(Collectors.groupingBy(ai -> ai.target().asMethodParameter().position()));

        short gitHubEventPosition = -1;
        short issueCommentPayloadPosition = -1;

        for (short i = 0; i < originalMethodParameterTypes.size(); i++) {
            if (GitHubAppDotNames.GITHUB_EVENT.equals(originalMethodParameterTypes.get(i).name())) {
                gitHubEventPosition = i;
            }
            if (GH_EVENT_PAYLOAD_ISSUE_COMMENT.equals(originalMethodParameterTypes.get(i).name())) {
                issueCommentPayloadPosition = i;
            }
        }

        List<ClassDesc> parameterTypes = new ArrayList<>();
        originalMethodParameterTypes.stream().map(t -> classDescOf(t.name()))
                .forEach(parameterTypes::add);
        if (gitHubEventPosition < 0) {
            parameterTypes.add(classDescOf(GitHubAppDotNames.GITHUB_EVENT));
            gitHubEventPosition = (short) (parameterTypes.size() - 1);
        }
        if (issueCommentPayloadPosition < 0) {
            parameterTypes.add(classDescOf(GH_EVENT_PAYLOAD_ISSUE_COMMENT));
            issueCommentPayloadPosition = (short) (parameterTypes.size() - 1);
        }

        final short finalGitHubEventPosition = gitHubEventPosition;
        final short finalIssueCommentPayloadPosition = issueCommentPayloadPosition;

        cc.method("dispatch", mc -> {
            mc.public_();
            mc.returning(void.class);

            // Create parameters
            List<ParamVar> params = new ArrayList<>();
            for (short i = 0; i < parameterTypes.size(); i++) {
                final short paramIndex = i;
                ParamVar param = mc.parameter("param" + i, pc -> {
                    pc.setType(parameterTypes.get(paramIndex));

                    // Add annotations
                    if (paramIndex < originalMethodParameterTypes.size()) {
                        List<AnnotationInstance> annotations = originalMethodParameterAnnotationMapping.getOrDefault(paramIndex,
                                Collections.emptyList());
                        if (paramIndex == finalIssueCommentPayloadPosition) {
                            // enforce issue_comment.created for the IssueComment payload
                            pc.addAnnotation(IssueComment.Created.class);
                        } else {
                            // copy the annotations
                            for (AnnotationInstance annotation : annotations) {
                                Jandex2Gizmo.addAnnotation(pc, annotation, index);
                            }
                        }
                    } else if (paramIndex == finalIssueCommentPayloadPosition) {
                        pc.addAnnotation(IssueComment.Created.class);
                    }
                });
                params.add(param);
            }

            mc.body(b0 -> {
                Expr thisExpr = mc.this_();
                Expr issueCommentPayloadExpr = params.get(finalIssueCommentPayloadPosition);
                Expr gitHubEventExpr = params.get(finalGitHubEventPosition);

                LocalVar commandExecutionContextOptional = b0.localVar("commandExecutionContextOptional", Optional.class,
                        b0.invokeSpecial(
                                MethodDesc.of(AbstractCommandDispatcher.class, "getCommand", Optional.class,
                                        GitHubEvent.class, GHEventPayload.IssueComment.class),
                                thisExpr, gitHubEventExpr, issueCommentPayloadExpr));

                b0.ifElse(
                        b0.invokeVirtual(MethodDesc.of(Optional.class, "isPresent", boolean.class),
                                commandExecutionContextOptional),
                        b1 -> {
                            // True branch - command is present
                            LocalVar commandExecutionContextVar = b1.localVar("commandExecutionContext", Object.class,
                                    b1.invokeVirtual(
                                            MethodDesc.of(Optional.class, "get", Object.class),
                                            commandExecutionContextOptional));
                            LocalVar commandVar = b1.localVar("command", Object.class,
                                    b1.invokeVirtual(
                                            MethodDesc.of(CommandExecutionContext.class, "getCommand", Object.class),
                                            commandExecutionContextVar));
                            LocalVar ackReactionVar = b1.localVar("ackReaction", GHReaction.class,
                                    b1.invokeVirtual(
                                            MethodDesc.of(CommandExecutionContext.class, "getAckReaction", GHReaction.class),
                                            commandExecutionContextVar));
                            LocalVar reactionStrategyVar = b1.localVar("reactionStrategy", ReactionStrategy.class,
                                    b1.invokeVirtual(
                                            MethodDesc.of(CommandConfig.class, "getReactionStrategy", ReactionStrategy.class),
                                            b1.invokeVirtual(
                                                    MethodDesc.of(CommandExecutionContext.class, "getCommandConfig",
                                                            CommandConfig.class),
                                                    commandExecutionContextVar)));

                            b1.try_(tc -> {
                                tc.body(b2 -> {
                                    // Build run method parameters
                                    List<Expr> runMethodParameters = new ArrayList<>();
                                    for (int i = 0; i < originalMethodParameterTypes.size(); i++) {
                                        runMethodParameters.add(params.get(i));
                                    }

                                    // Call the run method
                                    ClassDesc runMethodOwner = classDescOf(runMethod.getClazz().name());
                                    ClassDesc runMethodReturnType = classDescOf(runMethod.getMethod().returnType().name());
                                    ClassDesc[] runMethodParamTypes = runMethod.getMethod().parameterTypes().stream()
                                            .map(t -> classDescOf(t.name()))
                                            .toArray(ClassDesc[]::new);
                                    java.lang.constant.MethodTypeDesc runMethodTypeDesc = java.lang.constant.MethodTypeDesc
                                            .of(runMethodReturnType, runMethodParamTypes);
                                    MethodDesc runMethodDesc = io.quarkus.gizmo2.desc.InterfaceMethodDesc.of(
                                            runMethodOwner,
                                            runMethod.getMethod().name(),
                                            runMethodTypeDesc);

                                    b2.invokeInterface(runMethodDesc, commandVar,
                                            runMethodParameters.toArray(new Expr[0]));

                                    b2.invokeSpecial(MethodDesc.of(AbstractCommandDispatcher.class, "handleSuccess",
                                            void.class, GitHubEvent.class, GHEventPayload.IssueComment.class,
                                            CommandExecutionContext.class),
                                            thisExpr, gitHubEventExpr, issueCommentPayloadExpr, commandExecutionContextVar);
                                    deleteReaction(b2, issueCommentPayloadExpr, ackReactionVar);

                                    b2.if_(b2.invokeVirtual(
                                            MethodDesc.of(ReactionStrategy.class, "reactionOnNormalFlow", boolean.class),
                                            reactionStrategyVar),
                                            b3 -> createReaction(b3, issueCommentPayloadExpr, ReactionContent.PLUS_ONE));
                                });

                                tc.catch_(Exception.class, "e", (b2, caughtException) -> {
                                    deleteReaction(b2, issueCommentPayloadExpr, ackReactionVar);

                                    b2.if_(b2.invokeVirtual(
                                            MethodDesc.of(ReactionStrategy.class, "reactionOnError", boolean.class),
                                            reactionStrategyVar),
                                            b3 -> createReaction(b3, issueCommentPayloadExpr, ReactionContent.MINUS_ONE));

                                    b2.invokeSpecial(MethodDesc.of(AbstractCommandDispatcher.class, "handleExecutionError",
                                            void.class, GitHubEvent.class, GHEventPayload.IssueComment.class,
                                            CommandExecutionContext.class,
                                            Exception.class), thisExpr,
                                            gitHubEventExpr, issueCommentPayloadExpr, commandExecutionContextVar,
                                            caughtException);

                                    b2.throw_(b2.new_(
                                            ConstructorDesc.of(CommandExecutionException.class, String.class, Exception.class),
                                            stringFormat(b2, Const.of("Unable to execute command: %1$s"),
                                                    b2.invokeVirtual(
                                                            MethodDesc.of(GHIssueComment.class, "getBody", String.class),
                                                            getIssueComment(b2, issueCommentPayloadExpr))),
                                            caughtException));
                                });
                            });
                        },
                        b1 -> {
                            // False branch - command is not present, just return
                            b1.return_();
                        });

                b0.return_();
            });
        });
    }

    private static void generateCommandDispatcherGetCommandConfigsMethod(io.quarkus.gizmo2.creator.ClassCreator cc,
            IndexView index,
            Collection<ClassInfo> allCommands) {
        cc.method("getCommandConfigs", mc -> {
            mc.protected_();
            mc.returning(Map.class);
            mc.body(bc -> {
                LocalVar commandConfigsVar = bc.localVar("commandConfigs", HashMap.class,
                        bc.new_(ConstructorDesc.of(HashMap.class)));

                for (ClassInfo command : allCommands) {
                    AnnotationInstance commandOptionsAnnotation = command.declaredAnnotation(COMMAND_OPTIONS);

                    if (commandOptionsAnnotation != null) {
                        bc.invokeVirtual(
                                MethodDesc.of(HashMap.class, "put", Object.class, Object.class, Object.class),
                                commandConfigsVar,
                                Const.of(command.name().toString()),
                                getCommandConfig(bc, index, commandOptionsAnnotation));
                    }
                }

                bc.return_(commandConfigsVar);
            });
        });
    }

    private static void generateCommandDispatcherGetCommandPermissionConfigsMethod(io.quarkus.gizmo2.creator.ClassCreator cc,
            Collection<ClassInfo> allCommands) {
        cc.method("getCommandPermissionConfigs", mc -> {
            mc.protected_();
            mc.returning(Map.class);
            mc.body(bc -> {
                LocalVar commandPermissionConfigsVar = bc.localVar("commandPermissionConfigs", HashMap.class,
                        bc.new_(ConstructorDesc.of(HashMap.class)));

                for (ClassInfo command : allCommands) {
                    AnnotationInstance permissionAnnotation = command.declaredAnnotation(PERMISSION);

                    if (permissionAnnotation != null) {
                        bc.invokeVirtual(
                                MethodDesc.of(HashMap.class, "put", Object.class, Object.class, Object.class),
                                commandPermissionConfigsVar,
                                Const.of(command.name().toString()),
                                getCommandPermissionConfig(bc, permissionAnnotation));
                    }
                }

                bc.return_(commandPermissionConfigsVar);
            });
        });
    }

    private static Expr getCommandPermissionConfig(BlockCreator bc,
            AnnotationInstance permissionAnnotation) {
        Expr permissionExpr;
        if (permissionAnnotation != null) {
            permissionExpr = Const.of(GHPermissionType.valueOf(permissionAnnotation.value().asEnum()));
        } else {
            permissionExpr = Const.ofNull(GHPermissionType.class);
        }

        return bc.new_(
                ConstructorDesc.of(CommandPermissionConfig.class, GHPermissionType.class),
                permissionExpr);
    }

    private static void generateCommandDispatcherGetCommandTeamConfigsMethod(io.quarkus.gizmo2.creator.ClassCreator cc,
            Collection<ClassInfo> allCommands) {
        cc.method("getCommandTeamConfigs", mc -> {
            mc.protected_();
            mc.returning(Map.class);
            mc.body(bc -> {
                LocalVar commandTeamConfigsVar = bc.localVar("commandTeamConfigs", HashMap.class,
                        bc.new_(ConstructorDesc.of(HashMap.class)));

                for (ClassInfo command : allCommands) {
                    AnnotationInstance teamAnnotation = command.declaredAnnotation(TEAM);

                    if (teamAnnotation != null) {
                        bc.invokeVirtual(
                                MethodDesc.of(HashMap.class, "put", Object.class, Object.class, Object.class),
                                commandTeamConfigsVar,
                                Const.of(command.name().toString()),
                                getCommandTeamConfig(bc, teamAnnotation));
                    }
                }

                bc.return_(commandTeamConfigsVar);
            });
        });
    }

    private static Expr getCommandTeamConfig(BlockCreator bc, AnnotationInstance teamAnnotation) {
        Expr teamsExpr;
        if (teamAnnotation != null) {
            String[] teams = teamAnnotation.value().asStringArray();
            List<Expr> teamExprs = new ArrayList<>();
            for (String team : teams) {
                teamExprs.add(Const.of(team));
            }
            teamsExpr = bc.newArray(String.class, teamExprs);
        } else {
            teamsExpr = Const.ofNull(String[].class);
        }

        return bc.new_(
                ConstructorDesc.of(CommandTeamConfig.class, String[].class),
                teamsExpr);
    }

    private static void generateCommandDispatcherConstructor(io.quarkus.gizmo2.creator.ClassCreator cc,
            String commandDispatcherClassName, IndexView index, ClassInfo cliClassInfo, List<String> aliases) {
        cc.constructor(constr -> {
            constr.public_();
            constr.body(bc -> {
                Expr aliasesExpr = toExpr(bc, aliases);
                Expr cliConfigExpr;
                AnnotationInstance cliOptionsAnnotation = cliClassInfo.declaredAnnotation(CLI_OPTIONS);

                Expr defaultCommandConfigExpr = getCommandConfig(bc, index,
                        cliOptionsAnnotation != null
                                ? cliOptionsAnnotation.valueWithDefault(index, "defaultCommandOptions").asNested()
                                : null);
                Expr defaultCommandPermissionConfigExpr = getCommandPermissionConfig(bc,
                        cliClassInfo.declaredAnnotation(PERMISSION));
                Expr defaultCommandTeamConfigExpr = getCommandTeamConfig(bc,
                        cliClassInfo.declaredAnnotation(TEAM));

                if (cliOptionsAnnotation != null) {
                    cliConfigExpr = bc.new_(
                            ConstructorDesc.of(CliConfig.class, List.class,
                                    ParseErrorStrategy.class, String.class, Class.class, CommandConfig.class,
                                    CommandPermissionConfig.class, CommandTeamConfig.class),
                            aliasesExpr,
                            Const.of(ParseErrorStrategy
                                    .valueOf(cliOptionsAnnotation.valueWithDefault(index, "parseErrorStrategy").asEnum())),
                            Const.of(cliOptionsAnnotation.valueWithDefault(index, "parseErrorMessage").asString()),
                            Const.of(ClassDesc.of(
                                    cliOptionsAnnotation.valueWithDefault(index, "parseErrorHandler").asClass().name()
                                            .toString())),
                            defaultCommandConfigExpr, defaultCommandPermissionConfigExpr, defaultCommandTeamConfigExpr);
                } else {
                    cliConfigExpr = bc.new_(
                            ConstructorDesc.of(CliConfig.class, List.class, CommandConfig.class,
                                    CommandPermissionConfig.class, CommandTeamConfig.class),
                            aliasesExpr, defaultCommandConfigExpr, defaultCommandPermissionConfigExpr,
                            defaultCommandTeamConfigExpr);
                }

                bc.invokeSpecial(
                        ConstructorDesc.of(AbstractCommandDispatcher.class, Class.class, CliConfig.class),
                        cc.this_(),
                        Const.of(ClassDesc.of(cliClassInfo.name().toString())),
                        cliConfigExpr);

                bc.return_();
            });
        });
    }

    private static Map<DotName, ClassInfo> getAllCommands(IndexView index, AnnotationInstance annotationInstance) {
        Map<DotName, ClassInfo> allCommands = new HashMap<>();

        AnnotationValue commandsValue = annotationInstance.value("commands");
        if (commandsValue != null) {
            for (Type commandType : commandsValue.asClassArray()) {
                allCommands.put(commandType.name(), index.getClassByName(commandType.name()));
            }
        }

        AnnotationValue defaultCommandValue = annotationInstance.value("defaultCommand");
        if (defaultCommandValue != null) {
            allCommands.put(defaultCommandValue.asClass().name(), index.getClassByName(defaultCommandValue.asClass().name()));
        }

        AnnotationValue groupsValue = annotationInstance.value("groups");
        if (groupsValue != null) {
            for (AnnotationInstance groupAnnotationInstance : groupsValue.asNestedArray()) {
                allCommands.putAll(getAllCommands(index, groupAnnotationInstance));
            }
        }

        return allCommands;
    }

    private static List<String> getAliases(AnnotationInstance cliAnnotationInstance) {
        List<String> cliAliases = new ArrayList<>();
        cliAliases.add(cliAnnotationInstance.value("name").asString());

        AnnotationInstance aliasAnnotation = cliAnnotationInstance.target().asClass().declaredAnnotation(ALIAS);
        if (aliasAnnotation != null) {
            cliAliases.add(aliasAnnotation.value("name").asString());
        }

        AnnotationInstance cliOptionsAnnotation = cliAnnotationInstance.target().asClass().declaredAnnotation(CLI_OPTIONS);
        if (cliOptionsAnnotation != null) {
            AnnotationValue aliases = cliOptionsAnnotation.value("aliases");
            if (aliases != null) {
                cliAliases.addAll(List.of(aliases.asStringArray()));
            }
        }

        return cliAliases;
    }

    private static Expr getCommandConfig(BlockCreator bc, IndexView index,
            AnnotationInstance commandOptionsAnnotation) {
        if (commandOptionsAnnotation == null) {
            return bc.new_(
                    ConstructorDesc.of(CommandConfig.class));
        }

        return bc.new_(
                ConstructorDesc.of(CommandConfig.class, CommandScope.class, ExecutionErrorStrategy.class,
                        String.class, Class.class, ReactionStrategy.class),
                Const.of(CommandScope.valueOf(commandOptionsAnnotation.valueWithDefault(index, "scope").asEnum())),
                Const.of(ExecutionErrorStrategy
                        .valueOf(commandOptionsAnnotation.valueWithDefault(index, "executionErrorStrategy").asEnum())),
                Const.of(commandOptionsAnnotation.valueWithDefault(index, "executionErrorMessage").asString()),
                Const.of(ClassDesc.of(
                        commandOptionsAnnotation.valueWithDefault(index, "executionErrorHandler").asClass().name()
                                .toString())),
                Const.of(ReactionStrategy
                        .valueOf(commandOptionsAnnotation.valueWithDefault(index, "reactionStrategy").asEnum())));
    }

    private static RunMethod findCommonInterfaceWithRunMethod(IndexView index, String cliName,
            Collection<ClassInfo> commands) {
        if (commands.isEmpty()) {
            throw new IllegalStateException("No commands are defined for command " + cliName);
        }

        List<DotName> candidates = commands.iterator().next().interfaceNames();
        for (ClassInfo command : commands) {
            candidates.retainAll(command.interfaceNames());
        }

        RunMethod commonInterfaceWithRunMethod = null;
        for (DotName candidate : candidates) {
            ClassInfo candidateClassInfo = index.getClassByName(candidate);

            if (candidateClassInfo == null) {
                continue;
            }

            List<MethodInfo> runMethods = candidateClassInfo.methods().stream()
                    .filter(mi -> RUN_METHOD.equals(mi.name()))
                    .collect(Collectors.toList());

            if (runMethods.isEmpty()) {
                continue;
            }
            if (runMethods.size() > 1) {
                throw new IllegalStateException(
                        "Found too many run(...) methods in interface common to all commands for command " + cliName + ": "
                                + candidate);
            }
            if (commonInterfaceWithRunMethod != null) {
                throw new IllegalStateException(
                        "Found too many interfaces with a run(...) method common to all commands for command " + cliName + ": "
                                + commonInterfaceWithRunMethod.getClazz() + " and " + candidate);
            }

            commonInterfaceWithRunMethod = new RunMethod(candidateClassInfo, runMethods.iterator().next());
        }

        if (commonInterfaceWithRunMethod == null) {
            throw new IllegalStateException(
                    "Unable to find an interface with a run(...) method common to all commands for command " + cliName);
        }

        return commonInterfaceWithRunMethod;
    }

    private static Expr getIssueComment(BlockCreator bc, Expr issueCommentPayloadExpr) {
        return bc.invokeVirtual(MethodDesc.of(GHEventPayload.IssueComment.class,
                "getComment", GHIssueComment.class), issueCommentPayloadExpr);
    }

    private static Expr createReaction(BlockCreator bc, Expr issueCommentPayloadExpr,
            ReactionContent reactionContent) {
        return bc.invokeStatic(
                MethodDesc.of(Reactions.class, "createReaction", GHReaction.class,
                        GHEventPayload.IssueComment.class,
                        ReactionContent.class),
                issueCommentPayloadExpr, Const.of(reactionContent));
    }

    private static void deleteReaction(BlockCreator bc, Expr issueCommentPayloadExpr,
            Expr reactionExpr) {
        bc.ifNotNull(reactionExpr, b -> b.invokeStatic(
                MethodDesc.of(Reactions.class, "deleteReaction", void.class, GHEventPayload.IssueComment.class,
                        GHReaction.class),
                issueCommentPayloadExpr, reactionExpr));
    }

    private static <T> Expr toExpr(BlockCreator bc, List<String> list) {
        List<Expr> listExprs = new ArrayList<>();
        for (String item : list) {
            listExprs.add(Const.of(item));
        }
        Expr arrayExpr = bc.newArray(String.class, listExprs);

        return bc.invokeStatic(MethodDesc.of(List.class, "of", List.class, Object[].class), arrayExpr);
    }

    private static Expr stringFormat(BlockCreator bc, Expr string, Expr... arguments) {
        List<Expr> argumentList = new ArrayList<>();
        for (Expr argument : arguments) {
            argumentList.add(argument);
        }
        Expr argumentsArrayExpr = bc.newArray(Object.class, argumentList);
        return bc.invokeStatic(
                MethodDesc.of(String.class, "format", String.class, String.class, Object[].class), string,
                argumentsArrayExpr);
    }

    private static class RunMethod {

        private final ClassInfo clazz;
        private final MethodInfo method;

        private RunMethod(ClassInfo clazz, MethodInfo method) {
            this.clazz = clazz;
            this.method = method;
        }

        public ClassInfo getClazz() {
            return clazz;
        }

        public MethodInfo getMethod() {
            return method;
        }
    }
}
