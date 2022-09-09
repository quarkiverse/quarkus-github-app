package io.quarkiverse.githubapp.command.airline.deployment;

import static io.quarkiverse.githubapp.command.airline.deployment.GitHubAppCommandAirlineDotNames.ALIAS;
import static io.quarkiverse.githubapp.command.airline.deployment.GitHubAppCommandAirlineDotNames.CLI;
import static io.quarkiverse.githubapp.command.airline.deployment.GitHubAppCommandAirlineDotNames.CLI_OPTIONS;
import static io.quarkiverse.githubapp.command.airline.deployment.GitHubAppCommandAirlineDotNames.COMMAND;
import static io.quarkiverse.githubapp.command.airline.deployment.GitHubAppCommandAirlineDotNames.COMMAND_OPTIONS;
import static io.quarkiverse.githubapp.command.airline.deployment.GitHubAppCommandAirlineDotNames.DEPENDENT;
import static io.quarkiverse.githubapp.command.airline.deployment.GitHubAppCommandAirlineDotNames.GH_EVENT_PAYLOAD_ISSUE_COMMENT;
import static io.quarkiverse.githubapp.command.airline.deployment.GitHubAppCommandAirlineDotNames.ISSUE_COMMENT_CREATED;
import static io.quarkiverse.githubapp.command.airline.deployment.GitHubAppCommandAirlineDotNames.PERMISSION;
import static io.quarkiverse.githubapp.command.airline.deployment.GitHubAppCommandAirlineDotNames.TEAM;

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
import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHEventPayload.IssueComment;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHPermissionType;
import org.kohsuke.github.GHReaction;
import org.kohsuke.github.ReactionContent;

import io.quarkiverse.githubapp.command.airline.CliOptions.ParseErrorStrategy;
import io.quarkiverse.githubapp.command.airline.CommandOptions.CommandScope;
import io.quarkiverse.githubapp.command.airline.CommandOptions.ExecutionErrorStrategy;
import io.quarkiverse.githubapp.command.airline.CommandOptions.ReactionStrategy;
import io.quarkiverse.githubapp.command.airline.runtime.AbstractCommandDispatcher;
import io.quarkiverse.githubapp.command.airline.runtime.AbstractCommandDispatcher.CommandExecutionContext;
import io.quarkiverse.githubapp.command.airline.runtime.CliConfig;
import io.quarkiverse.githubapp.command.airline.runtime.CommandConfig;
import io.quarkiverse.githubapp.command.airline.runtime.CommandExecutionException;
import io.quarkiverse.githubapp.command.airline.runtime.CommandPermissionConfig;
import io.quarkiverse.githubapp.command.airline.runtime.CommandTeamConfig;
import io.quarkiverse.githubapp.command.airline.runtime.util.Reactions;
import io.quarkiverse.githubapp.deployment.AdditionalEventDispatchingClassesIndexBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.CatchBlockCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TryBlock;

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
            BuildProducer<AnnotationsTransformerBuildItem> annotationsTransformer) {
        beanDefiningAnnotations.produce(new BeanDefiningAnnotationBuildItem(COMMAND, DEPENDENT));

        annotationsTransformer
                .produce(new AnnotationsTransformerBuildItem(new HideAirlineInjectAnnotationsTransformer(index.getIndex())));
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
        ClassOutput classOutput = new GeneratedBeanGizmoAdaptor(indexedGeneratedBeans);

        for (AnnotationInstance cliAnnotationInstance : index.getIndex().getAnnotations(CLI)) {
            List<String> aliases = getAliases(cliAnnotationInstance);

            Map<DotName, ClassInfo> allCommands = getAllCommands(index.getIndex(), cliAnnotationInstance);
            for (ClassInfo commandClassInfo : allCommands.values()) {
                reflectiveClasses.produce(new ReflectiveClassBuildItem(true, true, commandClassInfo.toString()));
            }

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

        ClassCreator commandDispatcherClassCreator = ClassCreator.builder().classOutput(classOutput)
                .className(commandDispatcherClassName)
                .superClass(AbstractCommandDispatcher.class)
                .build();

        generateCommandDispatcherConstructor(commandDispatcherClassCreator, commandDispatcherClassName, index, cliClassInfo,
                aliases);

        generateCommandDispatcherGetCommandConfigsMethod(commandDispatcherClassCreator, index, allCommands);
        generateCommandDispatcherGetCommandPermissionConfigsMethod(commandDispatcherClassCreator, allCommands);
        generateCommandDispatcherGetCommandTeamConfigsMethod(commandDispatcherClassCreator, allCommands);

        generateCommandDispatcherDispatchMethod(commandDispatcherClassCreator, runMethod);

        commandDispatcherClassCreator.close();
    }

    private static void generateCommandDispatcherDispatchMethod(ClassCreator commandDispatcherClassCreator,
            RunMethod runMethod) {
        List<Type> originalMethodParameterTypes = runMethod.getMethod().parameterTypes();
        Map<Short, List<AnnotationInstance>> originalMethodParameterAnnotationMapping = runMethod.getMethod().annotations()
                .stream()
                .filter(ai -> ai.target().kind() == Kind.METHOD_PARAMETER)
                .collect(Collectors.groupingBy(ai -> ai.target().asMethodParameter().position()));

        short issueCommentPayloadPosition = -1;
        boolean originalMethodHasIssueCommentPayloadParameter = false;

        for (short i = 0; i < originalMethodParameterTypes.size(); i++) {
            if (GH_EVENT_PAYLOAD_ISSUE_COMMENT.equals(originalMethodParameterTypes.get(i).name())) {
                issueCommentPayloadPosition = i;
                originalMethodHasIssueCommentPayloadParameter = true;
            }
        }

        List<String> parameterTypes = new ArrayList<>();
        originalMethodParameterTypes.stream().map(t -> t.name().toString())
                .forEach(parameterTypes::add);
        if (!originalMethodHasIssueCommentPayloadParameter) {
            parameterTypes.add(GH_EVENT_PAYLOAD_ISSUE_COMMENT.toString());
            issueCommentPayloadPosition = (short) (parameterTypes.size() - 1);
        }

        MethodCreator dispatchMethodCreator = commandDispatcherClassCreator.getMethodCreator("dispatch",
                void.class.getName(), parameterTypes.toArray());
        for (short i = 0; i < originalMethodParameterTypes.size(); i++) {
            List<AnnotationInstance> annotations = originalMethodParameterAnnotationMapping.getOrDefault(i,
                    Collections.emptyList());
            if (i == issueCommentPayloadPosition) {
                // enforce issue_comment.created for the IssueComment payload
                dispatchMethodCreator.getParameterAnnotations(i)
                        .addAnnotation(ISSUE_COMMENT_CREATED.toString());
            } else {
                // copy the annotations
                for (AnnotationInstance annotation : annotations) {
                    dispatchMethodCreator.getParameterAnnotations(i)
                            .addAnnotation(annotation);
                }
            }
        }
        if (!originalMethodHasIssueCommentPayloadParameter) {
            dispatchMethodCreator.getParameterAnnotations(issueCommentPayloadPosition)
                    .addAnnotation(ISSUE_COMMENT_CREATED.toString());
        }

        ResultHandle issueCommentPayloadRh = dispatchMethodCreator.getMethodParam(issueCommentPayloadPosition);

        ResultHandle commandExecutionContextOptional = dispatchMethodCreator.invokeSpecialMethod(
                MethodDescriptor.ofMethod(AbstractCommandDispatcher.class, "getCommand", Optional.class,
                        GHEventPayload.IssueComment.class),
                dispatchMethodCreator.getThis(), issueCommentPayloadRh);
        BranchResult commandExecutionContextOptionalIsPresent = dispatchMethodCreator.ifTrue(dispatchMethodCreator
                .invokeVirtualMethod(MethodDescriptor.ofMethod(Optional.class, "isPresent", boolean.class),
                        commandExecutionContextOptional));

        commandExecutionContextOptionalIsPresent.falseBranch().returnValue(null);

        BytecodeCreator commandExecutionContextOptionalIsPresentTrue = commandExecutionContextOptionalIsPresent.trueBranch();
        ResultHandle commandExecutionContextRh = commandExecutionContextOptionalIsPresentTrue
                .invokeVirtualMethod(MethodDescriptor.ofMethod(Optional.class, "get", Object.class),
                        commandExecutionContextOptional);
        ResultHandle commandRh = commandExecutionContextOptionalIsPresentTrue
                .invokeVirtualMethod(MethodDescriptor.ofMethod(CommandExecutionContext.class, "getCommand", Object.class),
                        commandExecutionContextRh);
        ResultHandle ackReactionRh = commandExecutionContextOptionalIsPresentTrue
                .invokeVirtualMethod(
                        MethodDescriptor.ofMethod(CommandExecutionContext.class, "getAckReaction", GHReaction.class),
                        commandExecutionContextRh);
        ResultHandle reactionStrategyRh = commandExecutionContextOptionalIsPresentTrue
                .invokeVirtualMethod(
                        MethodDescriptor.ofMethod(CommandConfig.class, "getReactionStrategy", ReactionStrategy.class),
                        commandExecutionContextOptionalIsPresentTrue
                                .invokeVirtualMethod(
                                        MethodDescriptor.ofMethod(CommandExecutionContext.class, "getCommandConfig",
                                                CommandConfig.class),
                                        commandExecutionContextRh));

        TryBlock tryBlock = commandExecutionContextOptionalIsPresentTrue.tryBlock();

        List<ResultHandle> runMethodParameters = new ArrayList<>();
        for (int i = 0; i < originalMethodParameterTypes.size(); i++) {
            runMethodParameters.add(commandExecutionContextOptionalIsPresentTrue.getMethodParam(i));
        }
        tryBlock.invokeInterfaceMethod(runMethod.getMethod(), commandRh,
                runMethodParameters.toArray(new ResultHandle[0]));
        deleteReaction(tryBlock, issueCommentPayloadRh, ackReactionRh);
        BranchResult reactionOnNormalFlow = tryBlock.ifTrue(tryBlock.invokeVirtualMethod(
                MethodDescriptor.ofMethod(ReactionStrategy.class, "reactionOnNormalFlow", boolean.class), reactionStrategyRh));
        createReaction(reactionOnNormalFlow.trueBranch(), issueCommentPayloadRh, ReactionContent.PLUS_ONE);

        CatchBlockCreator catchBlock = tryBlock.addCatch(Exception.class);
        deleteReaction(catchBlock, issueCommentPayloadRh, ackReactionRh);
        BranchResult reactionOnError = catchBlock.ifTrue(catchBlock.invokeVirtualMethod(
                MethodDescriptor.ofMethod(ReactionStrategy.class, "reactionOnError", boolean.class), reactionStrategyRh));
        createReaction(reactionOnError.trueBranch(), issueCommentPayloadRh, ReactionContent.MINUS_ONE);

        catchBlock.invokeSpecialMethod(MethodDescriptor.ofMethod(AbstractCommandDispatcher.class, "handleExecutionError",
                void.class, GHEventPayload.IssueComment.class, CommandExecutionContext.class), catchBlock.getThis(),
                issueCommentPayloadRh, commandExecutionContextRh);

        catchBlock.throwException(catchBlock
                .newInstance(MethodDescriptor.ofConstructor(CommandExecutionException.class, String.class, Exception.class),
                        stringFormat(catchBlock, catchBlock.load("Unable to execute command: %1$s"),
                                catchBlock.invokeVirtualMethod(
                                        MethodDescriptor.ofMethod(GHIssueComment.class, "getBody", String.class),
                                        getIssueComment(catchBlock, issueCommentPayloadRh))),
                        catchBlock.getCaughtException()));

        dispatchMethodCreator.returnValue(null);
    }

    private static void generateCommandDispatcherGetCommandConfigsMethod(ClassCreator commandDispatcherClassCreator,
            IndexView index,
            Collection<ClassInfo> allCommands) {
        MethodCreator getCommandConfigsMethodCreator = commandDispatcherClassCreator.getMethodCreator("getCommandConfigs",
                Map.class.getName());

        ResultHandle commandConfigsRh = getCommandConfigsMethodCreator
                .newInstance(MethodDescriptor.ofConstructor(HashMap.class));

        for (ClassInfo command : allCommands) {
            AnnotationInstance commandOptionsAnnotation = command.classAnnotation(COMMAND_OPTIONS);

            if (commandOptionsAnnotation != null) {
                getCommandConfigsMethodCreator.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(HashMap.class, "put", Object.class, Object.class, Object.class),
                        commandConfigsRh,
                        getCommandConfigsMethodCreator.load(command.name().toString()),
                        getCommandConfig(getCommandConfigsMethodCreator, index, commandOptionsAnnotation));
            }
        }

        getCommandConfigsMethodCreator.returnValue(commandConfigsRh);
    }

    private static void generateCommandDispatcherGetCommandPermissionConfigsMethod(ClassCreator commandDispatcherClassCreator,
            Collection<ClassInfo> allCommands) {
        MethodCreator getCommandPermissionConfigsMethodCreator = commandDispatcherClassCreator.getMethodCreator(
                "getCommandPermissionConfigs",
                Map.class.getName());

        ResultHandle commandPermissionConfigsRh = getCommandPermissionConfigsMethodCreator
                .newInstance(MethodDescriptor.ofConstructor(HashMap.class));

        for (ClassInfo command : allCommands) {
            AnnotationInstance permissionAnnotation = command.classAnnotation(PERMISSION);

            if (permissionAnnotation != null) {
                getCommandPermissionConfigsMethodCreator.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(HashMap.class, "put", Object.class, Object.class, Object.class),
                        commandPermissionConfigsRh,
                        getCommandPermissionConfigsMethodCreator.load(command.name().toString()),
                        getCommandPermissionConfig(getCommandPermissionConfigsMethodCreator, permissionAnnotation));
            }
        }

        getCommandPermissionConfigsMethodCreator.returnValue(commandPermissionConfigsRh);
    }

    private static ResultHandle getCommandPermissionConfig(MethodCreator bytecodeCreator,
            AnnotationInstance permissionAnnotation) {
        ResultHandle permissionRh;
        if (permissionAnnotation != null) {
            permissionRh = bytecodeCreator.load(GHPermissionType.valueOf(permissionAnnotation.value().asEnum()));
        } else {
            permissionRh = bytecodeCreator.loadNull();
        }

        return bytecodeCreator.newInstance(
                MethodDescriptor.ofConstructor(CommandPermissionConfig.class, GHPermissionType.class),
                permissionRh);
    }

    private static void generateCommandDispatcherGetCommandTeamConfigsMethod(ClassCreator commandDispatcherClassCreator,
            Collection<ClassInfo> allCommands) {
        MethodCreator getCommandTeamConfigsMethodCreator = commandDispatcherClassCreator.getMethodCreator(
                "getCommandTeamConfigs",
                Map.class.getName());

        ResultHandle commandTeamConfigsRh = getCommandTeamConfigsMethodCreator
                .newInstance(MethodDescriptor.ofConstructor(HashMap.class));

        for (ClassInfo command : allCommands) {
            AnnotationInstance teamAnnotation = command.classAnnotation(TEAM);

            if (teamAnnotation != null) {
                getCommandTeamConfigsMethodCreator.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(HashMap.class, "put", Object.class, Object.class, Object.class),
                        commandTeamConfigsRh,
                        getCommandTeamConfigsMethodCreator.load(command.name().toString()),
                        getCommandTeamConfig(getCommandTeamConfigsMethodCreator, teamAnnotation));
            }
        }

        getCommandTeamConfigsMethodCreator.returnValue(commandTeamConfigsRh);
    }

    private static ResultHandle getCommandTeamConfig(BytecodeCreator bytecodeCreator, AnnotationInstance teamAnnotation) {
        ResultHandle teamsRh;
        if (teamAnnotation != null) {
            String[] teams = teamAnnotation.value().asStringArray();
            teamsRh = bytecodeCreator.newArray(String.class, teams.length);

            for (int i = 0; i < teams.length; i++) {
                bytecodeCreator.writeArrayValue(teamsRh, i, bytecodeCreator.load(teams[i]));
            }
        } else {
            teamsRh = bytecodeCreator.loadNull();
        }

        return bytecodeCreator.newInstance(
                MethodDescriptor.ofConstructor(CommandTeamConfig.class, String[].class),
                teamsRh);
    }

    private static void generateCommandDispatcherConstructor(ClassCreator commandDispatcherClassCreator,
            String commandDispatcherClassName, IndexView index, ClassInfo cliClassInfo, List<String> aliases) {
        MethodCreator constructorMethodCreator = commandDispatcherClassCreator
                .getMethodCreator(MethodDescriptor.ofConstructor(commandDispatcherClassName));

        ResultHandle aliasesRh = toResultHandle(constructorMethodCreator, aliases);
        ResultHandle cliConfigRh;
        AnnotationInstance cliOptionsAnnotation = cliClassInfo.classAnnotation(CLI_OPTIONS);

        ResultHandle defaultCommandConfigRh = getCommandConfig(constructorMethodCreator, index,
                cliOptionsAnnotation != null ? cliOptionsAnnotation.valueWithDefault(index, "defaultCommandOptions").asNested()
                        : null);
        ResultHandle defaultCommandPermissionConfigRh = getCommandPermissionConfig(constructorMethodCreator,
                cliClassInfo.classAnnotation(PERMISSION));
        ResultHandle defaultCommandTeamConfigRh = getCommandTeamConfig(constructorMethodCreator,
                cliClassInfo.classAnnotation(TEAM));

        if (cliOptionsAnnotation != null) {
            cliConfigRh = constructorMethodCreator.newInstance(
                    MethodDescriptor.ofConstructor(CliConfig.class, List.class,
                            ParseErrorStrategy.class, String.class, CommandConfig.class,
                            CommandPermissionConfig.class, CommandTeamConfig.class),
                    aliasesRh,
                    constructorMethodCreator.load(ParseErrorStrategy
                            .valueOf(cliOptionsAnnotation.valueWithDefault(index, "parseErrorStrategy").asEnum())),
                    constructorMethodCreator
                            .load(cliOptionsAnnotation.valueWithDefault(index, "parseErrorMessage").asString()),
                    defaultCommandConfigRh, defaultCommandPermissionConfigRh, defaultCommandTeamConfigRh);
        } else {
            cliConfigRh = constructorMethodCreator.newInstance(
                    MethodDescriptor.ofConstructor(CliConfig.class, List.class, CommandConfig.class,
                            CommandPermissionConfig.class, CommandTeamConfig.class),
                    aliasesRh, defaultCommandConfigRh, defaultCommandPermissionConfigRh, defaultCommandTeamConfigRh);
        }

        constructorMethodCreator.invokeSpecialMethod(
                MethodDescriptor.ofConstructor(AbstractCommandDispatcher.class, Class.class, CliConfig.class),
                constructorMethodCreator.getThis(),
                constructorMethodCreator.loadClass(cliClassInfo),
                cliConfigRh);

        constructorMethodCreator.returnValue(null);
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

        AnnotationInstance aliasAnnotation = cliAnnotationInstance.target().asClass().classAnnotation(ALIAS);
        if (aliasAnnotation != null) {
            cliAliases.add(aliasAnnotation.value("name").asString());
        }

        AnnotationInstance cliOptionsAnnotation = cliAnnotationInstance.target().asClass().classAnnotation(CLI_OPTIONS);
        if (cliOptionsAnnotation != null) {
            AnnotationValue aliases = cliOptionsAnnotation.value("aliases");
            if (aliases != null) {
                cliAliases.addAll(List.of(aliases.asStringArray()));
            }
        }

        return cliAliases;
    }

    private static ResultHandle getCommandConfig(BytecodeCreator bytecodeCreator, IndexView index,
            AnnotationInstance commandOptionsAnnotation) {
        if (commandOptionsAnnotation == null) {
            return bytecodeCreator.newInstance(
                    MethodDescriptor.ofConstructor(CommandConfig.class));
        }

        return bytecodeCreator.newInstance(
                MethodDescriptor.ofConstructor(CommandConfig.class, CommandScope.class, ExecutionErrorStrategy.class,
                        String.class, ReactionStrategy.class),
                bytecodeCreator.load(CommandScope.valueOf(commandOptionsAnnotation.valueWithDefault(index, "scope").asEnum())),
                bytecodeCreator.load(ExecutionErrorStrategy
                        .valueOf(commandOptionsAnnotation.valueWithDefault(index, "executionErrorStrategy").asEnum())),
                bytecodeCreator.load(commandOptionsAnnotation.valueWithDefault(index, "executionErrorMessage").asString()),
                bytecodeCreator.load(ReactionStrategy
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

    private static ResultHandle getIssueComment(BytecodeCreator bytecodeCreator, ResultHandle issueCommentPayloadRh) {
        return bytecodeCreator.invokeVirtualMethod(MethodDescriptor.ofMethod(IssueComment.class,
                "getComment", GHIssueComment.class), issueCommentPayloadRh);
    }

    private static ResultHandle createReaction(BytecodeCreator bytecodeCreator, ResultHandle issueCommentPayloadRh,
            ReactionContent reactionContent) {
        return bytecodeCreator.invokeStaticMethod(
                MethodDescriptor.ofMethod(Reactions.class, "createReaction", GHReaction.class,
                        GHEventPayload.IssueComment.class,
                        ReactionContent.class),
                issueCommentPayloadRh, bytecodeCreator.load(reactionContent));
    }

    private static void deleteReaction(BytecodeCreator bytecodeCreator, ResultHandle issueCommentPayloadRh,
            ResultHandle reactionRh) {
        bytecodeCreator.ifNotNull(reactionRh).trueBranch().invokeStaticMethod(
                MethodDescriptor.ofMethod(Reactions.class, "deleteReaction", void.class, GHEventPayload.IssueComment.class,
                        GHReaction.class),
                issueCommentPayloadRh, reactionRh);
    }

    private static <T> ResultHandle toResultHandle(BytecodeCreator bytecodeCreator, List<String> list) {
        ResultHandle arrayRh = bytecodeCreator.newArray(String.class, list.size());
        for (int i = 0; i < list.size(); i++) {
            bytecodeCreator.writeArrayValue(arrayRh, i, bytecodeCreator.load(list.get(i)));
        }

        return bytecodeCreator
                .invokeStaticInterfaceMethod(MethodDescriptor.ofMethod(List.class, "of", List.class, Object[].class), arrayRh);
    }

    private static ResultHandle stringFormat(BytecodeCreator bytecodeCreator, ResultHandle string, ResultHandle... arguments) {
        ResultHandle argumentsArrayRh = bytecodeCreator.newArray(Object.class, arguments.length);
        for (ResultHandle argument : arguments) {
            bytecodeCreator.writeArrayValue(argumentsArrayRh, 0, argument);
        }
        return bytecodeCreator
                .invokeStaticMethod(
                        MethodDescriptor.ofMethod(String.class, "format", String.class, String.class, Object[].class), string,
                        argumentsArrayRh);
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
