/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.toolchain.tools;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import com.intellij.execution.configuration.EnvironmentVariablesData;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.registry.RegistryValue;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.execution.ParametersListUtil;
import com.intellij.util.net.HttpConfigurable;
import com.intellij.util.text.SemVer;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.rust.RsBundle;
import org.rust.cargo.CargoConfig;
import org.rust.cargo.CargoConstants;
import org.rust.cargo.CfgOptions;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.project.settings.RsProjectSettingsServiceUtil;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.cargo.project.workspace.CargoWorkspaceData;
import org.rust.cargo.runconfig.buildtool.CargoPatch;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;
import org.rust.cargo.toolchain.*;
import org.rust.cargo.toolchain.impl.BuildMessages;
import org.rust.cargo.toolchain.impl.CargoMetadata;
import org.rust.cargo.toolchain.impl.RustcMessage.CompilerMessage;
import org.rust.cargo.toolchain.impl.RustcVersion;
import org.rust.cargo.toolchain.wsl.RsWslToolchain;
import org.rust.cargo.util.ToolchainUtil;
import org.rust.ide.actions.InstallBinaryCrateAction;
import org.rust.ide.experiments.RsExperiments;
import org.rust.ide.notifications.NotificationUtils;
import org.rust.lang.RsConstants;
import org.rust.openapiext.*;
import org.rust.openapiext.JsonUtils;
import org.rust.stdext.RsResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A main gateway for executing cargo commands.
 * <p>
 * This class is not aware of SDKs or projects, so you'll need to provide
 * paths yourself.
 * <p>
 * It is impossible to guarantee that paths to the project or executables are valid,
 * because the user can always just rm ~/.cargo/bin -rf.
 */
public class Cargo extends RustupComponent {

    private static final Logger LOG = Logger.getInstance(Cargo.class);

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .registerModule(new KotlinModule.Builder().build());
    private static final TomlMapper TOML_MAPPER = new TomlMapper();

    public static final RegistryValue TEST_NOCAPTURE_ENABLED_KEY = Registry.get("org.rust.cargo.test.nocapture");
    public static final RegistryValue USE_BUILD_SCRIPT_WRAPPER = Registry.get("org.rust.cargo.evaluate.build.scripts.wrapper");

    public static final String NAME = "cargo";
    public static final String WRAPPER_NAME = "xargo";

    private static final List<String> FEATURES_ACCEPTING_COMMANDS = List.of(
        "bench", "build", "check", "doc", "fix", "run", "rustc", "rustdoc", "test", "metadata", "tree", "install", "package", "publish"
    );

    private static final List<String> COLOR_ACCEPTING_COMMANDS = List.of(
        "bench", "build", "check", "clean", "clippy", "doc", "install", "publish", "run", "rustc", "test", "update"
    );

    private static final SemVer RUST_1_62 = ToolchainUtil.parseSemVer("1.62.0");

    @Nullable
    private HttpConfigurable myHttp;

    public Cargo(RsToolchainBase toolchain, boolean useWrapper) {
        super(useWrapper ? WRAPPER_NAME : NAME, toolchain);
    }

    public Cargo(RsToolchainBase toolchain) {
        this(toolchain, false);
    }

    @org.jetbrains.annotations.NotNull
    public static Cargo cargo(@org.jetbrains.annotations.NotNull RsToolchainBase toolchain) {
        return new Cargo(toolchain);
    }

    @org.jetbrains.annotations.NotNull
    public static Cargo cargoOrWrapper(@org.jetbrains.annotations.NotNull RsToolchainBase toolchain, @Nullable java.nio.file.Path workingDirectory) {
        boolean useWrapper = toolchain.hasExecutable(WRAPPER_NAME);
        return new Cargo(toolchain, useWrapper);
    }

    public static final class BinaryCrate {
        private final String myName;
        @Nullable
        private final SemVer myVersion;

        public BinaryCrate(String name, @Nullable SemVer version) {
            myName = name;
            myVersion = version;
        }

        public String getName() { return myName; }
        @Nullable public SemVer getVersion() { return myVersion; }

        private static final java.util.regex.Pattern VERSION_LINE =
            java.util.regex.Pattern.compile("(?<name>[\\w-]+) v(?<version>\\d+\\.\\d+\\.\\d+(-[\\w.]+)?).*");

        @Nullable
        public static BinaryCrate from(String line) {
            java.util.regex.Matcher matcher = VERSION_LINE.matcher(line);
            if (!matcher.matches()) return null;
            String name = matcher.group("name");
            if (name == null) return null;
            String rawVersion = matcher.group("version");
            if (rawVersion == null) return null;
            return new BinaryCrate(name, SemVer.parseFromText(rawVersion));
        }
    }

    private List<BinaryCrate> listInstalledBinaryCrates() {
        ProcessOutput output = org.rust.openapiext.CommandLineExt.execute(
            createBaseCommandLine("install", "--list"),
            getToolchain().getExecutionTimeoutInMilliseconds());
        if (output == null) return Collections.emptyList();
        List<BinaryCrate> result = new ArrayList<>();
        for (String line : output.getStdoutLines()) {
            if (line.startsWith(" ")) continue;
            BinaryCrate crate = BinaryCrate.from(line);
            if (crate != null) result.add(crate);
        }
        return result;
    }

    public void installBinaryCrate(Project project, String crateName) {
        CargoProject cargoProject = CargoProjectServiceUtil.getCargoProjects(project).getAllProjects().stream().findFirst().orElse(null);
        if (cargoProject == null) return;
        CargoCommandLine commandLine = CargoCommandLine.forProject(cargoProject, "install", List.of("--force", crateName));
        commandLine.run(cargoProject, "Install " + crateName, false);
    }

    public void addDependency(Project project, String crateName, List<String> features) {
        CargoProject cargoProject = CargoProjectServiceUtil.getCargoProjects(project).getAllProjects().stream().findFirst().orElse(null);
        if (cargoProject == null) return;
        List<String> args = new ArrayList<>();
        args.add(crateName);
        if (!features.isEmpty()) {
            args.add("--features");
            args.add(String.join(",", features));
        }
        CargoCommandLine commandLine = CargoCommandLine.forProject(cargoProject, "add", args);
        commandLine.run(cargoProject, "Add dependency " + crateName, false);
    }

    public void addDependency(Project project, String crateName) {
        addDependency(project, crateName, Collections.emptyList());
    }

    public boolean checkSupportForBuildCheckAllTargets() {
        ProcessOutput output = org.rust.openapiext.CommandLineExt.execute(
            createBaseCommandLine("help", "check"),
            getToolchain().getExecutionTimeoutInMilliseconds());
        if (output == null) return false;
        for (String line : output.getStdoutLines()) {
            if (line.contains(" --all-targets ")) return true;
        }
        return false;
    }

    /**
     * Fetch all dependencies and calculate project information.
     */
    public RsResult<ProjectDescription, RsProcessExecutionOrDeserializationException> fullProjectDescription(
        Project owner,
        Path projectDirectory,
        List<String> buildTargets,
        @Nullable RustcVersion rustcVersion,
        java.util.function.Function<CargoCallType, ProcessListener> listenerProvider
    ) {
        RsResult<CargoMetadata.Project, RsProcessExecutionOrDeserializationException> rawDataResult =
            fetchMetadata(owner, projectDirectory, buildTargets, null, EnvironmentVariablesData.DEFAULT, listenerProvider.apply(CargoCallType.METADATA));
        if (rawDataResult instanceof RsResult.Err) {
            return new RsResult.Err<>(((RsResult.Err<?, RsProcessExecutionOrDeserializationException>) rawDataResult).err());
        }
        CargoMetadata.Project rawData = ((RsResult.Ok<CargoMetadata.Project, ?>) rawDataResult).ok();

        BuildMessages buildScriptsInfo;
        if (OpenApiUtil.isFeatureEnabled(RsExperiments.EVALUATE_BUILD_SCRIPTS)) {
            ProcessListener listener = listenerProvider.apply(CargoCallType.BUILD_SCRIPT_CHECK);
            buildScriptsInfo = fetchBuildScriptsInfo(owner, projectDirectory, rustcVersion, listener);
        } else {
            buildScriptsInfo = BuildMessages.DEFAULT;
        }

        Object[] replaced = replacePathsSymlinkIfNeeded(rawData, buildScriptsInfo, projectDirectory);
        CargoMetadata.Project rawDataAdjusted = (CargoMetadata.Project) replaced[0];
        BuildMessages buildScriptsInfoAdjusted = (BuildMessages) replaced[1];
        CargoWorkspaceData workspaceData = CargoMetadata.clean(rawDataAdjusted, buildScriptsInfoAdjusted);
        ProjectDescriptionStatus status = buildScriptsInfo.isSuccessful() ? ProjectDescriptionStatus.OK : ProjectDescriptionStatus.BUILD_SCRIPT_EVALUATION_ERROR;
        return new RsResult.Ok<>(new ProjectDescription(workspaceData, status));
    }

    public RsResult<CargoMetadata.Project, RsProcessExecutionOrDeserializationException> fetchMetadata(
        Project owner,
        Path projectDirectory,
        List<String> buildTargets,
        @Nullable String toolchainOverride,
        EnvironmentVariablesData environmentVariables,
        @Nullable ProcessListener listener
    ) {
        List<String> additionalArgs = new ArrayList<>(List.of("--verbose", "--format-version", "1", "--all-features"));
        for (String target : buildTargets) {
            additionalArgs.add("--filter-platform");
            additionalArgs.add(target);
        }

        CargoCommandLine cmdLine = new CargoCommandLine(
            "metadata", projectDirectory, additionalArgs, null,
            false, BacktraceMode.DEFAULT, toolchainOverride, RustChannel.DEFAULT,
            environmentVariables, true, false, false
        );

        RsResult<ProcessOutput, ?> execResult = executeCommandLine(cmdLine, owner, null, null, listener);
        if (execResult instanceof RsResult.Err) {
            return new RsResult.Err<>(((RsResult.Err<?, RsProcessExecutionOrDeserializationException>) execResult).err());
        }
        String stdout = ((RsResult.Ok<ProcessOutput, ?>) execResult).ok().getStdout();

        // Drop everything before first '{'
        int braceIdx = stdout.indexOf('{');
        String json = braceIdx >= 0 ? stdout.substring(braceIdx) : stdout;
        try {
            CargoMetadata.Project project = JSON_MAPPER.readValue(json, CargoMetadata.Project.class);
            CargoMetadata.Project converted = project.convertPaths(getToolchain()::toLocalPath);
            return new RsResult.Ok<>(converted);
        } catch (JacksonException e) {
            return new RsResult.Err<>(new RsDeserializationException(e));
        }
    }

    public RsResult<CargoMetadata.Project, RsProcessExecutionOrDeserializationException> fetchMetadata(
        Project owner,
        Path projectDirectory,
        List<String> buildTargets
    ) {
        return fetchMetadata(owner, projectDirectory, buildTargets, null, EnvironmentVariablesData.DEFAULT, null);
    }

    public RsResult<Void, org.rust.openapiext.RsProcessExecutionException> vendorDependencies(
        Project owner,
        Path projectDirectory,
        Path dstPath,
        @Nullable String toolchainOverride,
        EnvironmentVariablesData environmentVariables,
        @Nullable ProcessListener listener
    ) {
        List<String> additionalArgs = List.of("--respect-source-config", dstPath.toString());
        CargoCommandLine cmdLine = new CargoCommandLine(
            "vendor", projectDirectory, additionalArgs, null,
            false, BacktraceMode.DEFAULT, toolchainOverride, RustChannel.DEFAULT,
            environmentVariables, true, false, false
        );
        RsResult<ProcessOutput, ?> result = executeCommandLine(cmdLine, owner, null, null, listener);
        if (result instanceof RsResult.Err) {
            return new RsResult.Err<>(((RsResult.Err<?, org.rust.openapiext.RsProcessExecutionException>) result).err());
        }
        return new RsResult.Ok<>(null);
    }

    /**
     * Execute cargo rustc --print cfg and parse output as CfgOptions.
     * Available since Rust 1.52
     */
    public RsResult<CfgOptions, org.rust.openapiext.RsProcessExecutionException> getCfgOption(
        Project owner,
        @Nullable Path projectDirectory
    ) {
        GeneralCommandLine cmd = createBaseCommandLine(
            List.of("rustc", "-Z", "unstable-options", "--print", "cfg"),
            projectDirectory,
            Map.of(RsToolchainBase.RUSTC_BOOTSTRAP, "1")
        );
        RsResult<ProcessOutput, org.rust.openapiext.RsProcessExecutionException> result =
            CommandLineExt.execute(cmd, owner, null, null);
        if (result instanceof RsResult.Err) {
            return new RsResult.Err<>(((RsResult.Err<?, org.rust.openapiext.RsProcessExecutionException>) result).err());
        }
        ProcessOutput output = ((RsResult.Ok<ProcessOutput, ?>) result).ok();
        return new RsResult.Ok<>(CfgOptions.parse(output.getStdoutLines()));
    }

    /**
     * Execute cargo config get to get configuration as Jackson Tree.
     */
    public RsResult<CargoConfig, RsProcessExecutionOrDeserializationException> getConfig(
        Project owner,
        Path projectDirectory
    ) {
        List<String> parameters = List.of("-Z", "unstable-options", "config", "get");
        GeneralCommandLine cmd = createBaseCommandLine(
            parameters,
            projectDirectory,
            Map.of(RsToolchainBase.RUSTC_BOOTSTRAP, "1")
        );

        RsResult<ProcessOutput, ?> execResult = CommandLineExt.execute(cmd, owner, null, null);
        if (execResult instanceof RsResult.Err) {
            return new RsResult.Err<>(((RsResult.Err<?, RsProcessExecutionOrDeserializationException>) execResult).err());
        }
        String output = ((RsResult.Ok<ProcessOutput, ?>) execResult).ok().getStdout();

        JsonNode tree;
        try {
            tree = TOML_MAPPER.readTree(output);
        } catch (JacksonException e) {
            LOG.error(e);
            return new RsResult.Err<>(new RsDeserializationException(e));
        }

        Map<String, CargoConfig.EnvValue> env = new LinkedHashMap<>();
        JsonNode envNode = tree.at("/env");
        Iterator<Map.Entry<String, JsonNode>> fields = envNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            if (field.getValue().isTextual()) {
                env.put(field.getKey(), new CargoConfig.EnvValue(field.getValue().asText()));
            } else if (field.getValue().isObject()) {
                try {
                    CargoConfig.EnvValue valueParams = TOML_MAPPER.treeToValue(field.getValue(), CargoConfig.EnvValue.class);
                    env.put(field.getKey(), new CargoConfig.EnvValue(valueParams.value(), valueParams.isForced(), valueParams.isRelative()));
                } catch (JacksonException e) {
                    LOG.error(e);
                    return new RsResult.Err<>(new RsDeserializationException(e));
                }
            }
        }

        List<String> buildTargets = getBuildTargets(tree);
        List<String> resolvedTargets = new ArrayList<>();
        for (String target : buildTargets) {
            if (target.endsWith(".json")) {
                resolvedTargets.add(com.intellij.openapi.util.io.FileUtil.toSystemIndependentName(projectDirectory.resolve(target).toAbsolutePath().toString()));
            } else {
                resolvedTargets.add(target);
            }
        }
        return new RsResult.Ok<>(new CargoConfig(resolvedTargets, env));
    }

    private List<String> getBuildTargets(JsonNode tree) {
        JsonNode buildTargetNode = tree.at("/build/target");
        if (buildTargetNode.isTextual()) return Collections.singletonList(buildTargetNode.asText());
        if (buildTargetNode.isArray()) {
            List<String> targets = new ArrayList<>();
            for (JsonNode node : buildTargetNode) {
                targets.add(node.asText());
            }
            return targets;
        }
        return Collections.emptyList();
    }

    private BuildMessages fetchBuildScriptsInfo(
        Project owner,
        Path projectDirectory,
        @Nullable RustcVersion rustcVersion,
        @Nullable ProcessListener listener
    ) {
        List<String> additionalArgs = new ArrayList<>(List.of("--message-format", "json", "--workspace", "--all-targets"));
        boolean useKeepGoing = rustcVersion != null && rustcVersion.getSemver().compareTo(RUST_1_62) >= 0;
        Map<String, String> envMap = new HashMap<>();
        if (useKeepGoing) {
            additionalArgs.addAll(List.of("-Z", "unstable-options", "--keep-going"));
            String originalRustcBootstrapValue = System.getenv(RsToolchainBase.RUSTC_BOOTSTRAP);
            envMap.put(RsToolchainBase.RUSTC_BOOTSTRAP, "1");
            if (originalRustcBootstrapValue != null) {
                envMap.put(RsToolchainBase.ORIGINAL_RUSTC_BOOTSTRAP, originalRustcBootstrapValue);
            }
        }
        Path nativeHelper = RsPathManager.nativeHelper(getToolchain() instanceof RsWslToolchain);
        if (nativeHelper != null && USE_BUILD_SCRIPT_WRAPPER.asBoolean()) {
            envMap.put(RsToolchainBase.RUSTC_WRAPPER, nativeHelper.toString());
        }

        EnvironmentVariablesData envs = EnvironmentVariablesData.create(envMap, true);
        CargoCommandLine commandLine = new CargoCommandLine("check", projectDirectory, additionalArgs, null,
            false, BacktraceMode.DEFAULT, null, RustChannel.DEFAULT, envs, true, false, false);

        RsResult<ProcessOutput, org.rust.openapiext.RsProcessExecutionException> processResult = executeCommandLine(commandLine, owner, null, null, listener);
        if (processResult instanceof RsResult.Err) {
            LOG.warn("Build script evaluation failed: " + ((RsResult.Err<?, ?>) processResult).err());
        }

        RsResult<ProcessOutput, org.rust.openapiext.RsProcessExecutionException.Start> ignoredResult = RsProcessResultUtil.ignoreExitCode(processResult);
        if (ignoredResult instanceof RsResult.Err) {
            return BuildMessages.FAILED;
        }
        ProcessOutput processOutput = ((RsResult.Ok<ProcessOutput, ?>) ignoredResult).ok();

        Map<String, List<CompilerMessage>> messages = new HashMap<>();
        for (String line : processOutput.getStdoutLines()) {
            com.google.gson.JsonObject jsonObject = JsonUtils.tryParseJsonObject(line);
            if (jsonObject == null) continue;
            CompilerMessage msg = CompilerMessage.fromJson(jsonObject);
            if (msg != null) {
                CompilerMessage converted = msg.convertPaths(getToolchain()::toLocalPath);
                messages.computeIfAbsent(converted.getPackageId(), k -> new ArrayList<>()).add(converted);
            }
        }
        return new BuildMessages(messages, processOutput.getExitCode() == 0);
    }

    private Object[] replacePathsSymlinkIfNeeded(
        CargoMetadata.Project project,
        @Nullable BuildMessages buildMessages,
        Path projectDirectoryRel
    ) {
        Path projectDirectory = projectDirectoryRel.toAbsolutePath();
        String workspaceRoot = project.getWorkspace_root();

        if (projectDirectory.toString().equals(workspaceRoot)) {
            return new Object[]{project, buildMessages};
        }

        Path workspaceRootPath = Paths.get(workspaceRoot);
        try {
            if (!Files.isSameFile(projectDirectory, workspaceRootPath)) {
                return new Object[]{project, buildMessages};
            }
        } catch (java.io.IOException e) {
            return new Object[]{project, buildMessages};
        }

        String normalisedWorkspace = projectDirectory.normalize().toString();
        java.util.function.Function<String, String> replacer = path -> {
            if (!path.startsWith(workspaceRoot)) return path;
            return normalisedWorkspace + path.substring(workspaceRoot.length());
        };
        return new Object[]{
            project.replacePaths(replacer),
            buildMessages != null ? buildMessages.replacePaths(replacer) : null
        };
    }

    public RsResult<GeneratedFilesHolder, org.rust.openapiext.RsProcessExecutionException> init(
        Project project,
        Disposable owner,
        VirtualFile directory,
        String name,
        boolean createBinary,
        @Nullable String vcs
    ) {
        Path path = Paths.get(directory.getPath());
        String crateType = createBinary ? "--bin" : "--lib";
        List<String> args = new ArrayList<>(List.of(crateType, "--name", name));
        if (vcs != null) {
            args.addAll(List.of("--vcs", vcs));
        }
        args.add(path.toString());

        CargoCommandLine cmdLine = new CargoCommandLine("init", path, args);
        RsResult<ProcessOutput, ?> result = executeCommandLine(cmdLine, project, owner, null, null);
        if (result instanceof RsResult.Err) {
            return new RsResult.Err<>(((RsResult.Err<?, org.rust.openapiext.RsProcessExecutionException>) result).err());
        }
        OpenApiUtil.fullyRefreshDirectory(directory);

        VirtualFile manifest = directory.findChild(CargoConstants.MANIFEST_FILE);
        if (manifest == null) throw new IllegalStateException("Can't find the manifest file");
        String fileName = createBinary ? RsConstants.MAIN_RS_FILE : RsConstants.LIB_RS_FILE;
        VirtualFile srcFile = directory.findFileByRelativePath("src/" + fileName);
        List<VirtualFile> sourceFiles = srcFile != null ? List.of(srcFile) : Collections.emptyList();
        return new RsResult.Ok<>(new GeneratedFilesHolder(manifest, sourceFiles));
    }

    public RsResult<GeneratedFilesHolder, org.rust.openapiext.RsProcessExecutionException> init(
        Project project,
        Disposable owner,
        VirtualFile directory,
        String name,
        boolean createBinary
    ) {
        return init(project, owner, directory, name, createBinary, null);
    }

    public RsResult<GeneratedFilesHolder, org.rust.openapiext.RsProcessExecutionException> generate(
        Project project,
        Disposable owner,
        VirtualFile directory,
        String name,
        String templateUrl,
        @Nullable String vcs
    ) {
        Path path = Paths.get(directory.getPath());
        List<String> args = new ArrayList<>(List.of("--name", name, "--git", templateUrl, "--init", "--force"));
        if (vcs != null) {
            args.addAll(List.of("--vcs", vcs));
        }

        CargoCommandLine cmdLine = new CargoCommandLine("generate", path, args);
        RsResult<ProcessOutput, ?> result = executeCommandLine(cmdLine, project, owner, null, null);
        if (result instanceof RsResult.Err) {
            return new RsResult.Err<>(((RsResult.Err<?, org.rust.openapiext.RsProcessExecutionException>) result).err());
        }
        OpenApiUtil.fullyRefreshDirectory(directory);

        VirtualFile manifest = directory.findChild(CargoConstants.MANIFEST_FILE);
        if (manifest == null) throw new IllegalStateException("Can't find the manifest file");
        List<VirtualFile> sourceFiles = new ArrayList<>();
        for (String srcName : List.of("main", "lib")) {
            VirtualFile f = directory.findFileByRelativePath("src/" + srcName + ".rs");
            if (f != null) sourceFiles.add(f);
        }
        return new RsResult.Ok<>(new GeneratedFilesHolder(manifest, sourceFiles));
    }

    public RsResult<ProcessOutput, org.rust.openapiext.RsProcessExecutionException.Start> checkProject(
        Project project,
        Disposable owner,
        CargoCheckArgs args
    ) {
        boolean useClippy = args.getLinter() == ExternalLinter.CLIPPY
            && !Rustup.checkNeedInstallClippy(project, args.getCargoProjectDirectory());
        String checkCommand = useClippy ? "clippy" : "check";

        CargoCommandLine commandLine;
        if (args instanceof CargoCheckArgs.SpecificTarget) {
            CargoCheckArgs.SpecificTarget specific = (CargoCheckArgs.SpecificTarget) args;
            List<String> arguments = new ArrayList<>();
            arguments.add("--message-format=json");
            arguments.add("--no-default-features");
            Map<String, org.rust.cargo.project.workspace.FeatureState> featureState = specific.getTarget().getPkg().getFeatureState();
            List<String> enabledFeatures = featureState.entrySet().stream()
                .filter(e -> e.getValue().isEnabled())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
            if (!enabledFeatures.isEmpty()) {
                arguments.add("--features");
                arguments.add(String.join(" ", enabledFeatures));
            }
            if (!(specific.getTarget().getKind() instanceof CargoWorkspace.TargetKind.Test)) {
                arguments.add("--tests");
            }
            arguments.addAll(ParametersListUtil.parse(specific.getExtraArguments()));
            commandLine = CargoCommandLine.forTarget(
                specific.getTarget(),
                checkCommand,
                arguments,
                specific.getChannel(),
                EnvironmentVariablesData.create(specific.getEnvs(), true),
                false
            );
        } else {
            CargoCheckArgs.FullWorkspace fullWs = (CargoCheckArgs.FullWorkspace) args;
            List<String> arguments = new ArrayList<>();
            arguments.add("--message-format=json");
            arguments.add("--all");
            if (fullWs.getAllTargets() && checkSupportForBuildCheckAllTargets()) {
                arguments.add("--all-targets");
            }
            arguments.addAll(ParametersListUtil.parse(fullWs.getExtraArguments()));
            commandLine = new CargoCommandLine(
                checkCommand,
                fullWs.getCargoProjectDirectory(),
                arguments,
                null,
                false,
                BacktraceMode.DEFAULT,
                null,
                fullWs.getChannel(),
                EnvironmentVariablesData.create(fullWs.getEnvs(), true),
                true,
                false,
                false
            );
        }

        RsResult<ProcessOutput, org.rust.openapiext.RsProcessExecutionException> result = executeCommandLine(commandLine, project, owner, null, null);
        return RsProcessResultUtil.ignoreExitCode(result);
    }

    public GeneralCommandLine toColoredCommandLine(Project project, CargoCommandLine commandLine) {
        return toGeneralCommandLine(project, commandLine, true);
    }

    public GeneralCommandLine toGeneralCommandLine(Project project, CargoCommandLine commandLine) {
        return toGeneralCommandLine(project, commandLine, false);
    }

    private GeneralCommandLine toGeneralCommandLine(Project project, CargoCommandLine commandLine, boolean colors) {
        CargoCommandLine patched = patchArgs(commandLine, project, colors);

        List<String> parameters = new ArrayList<>();
        if (patched.getChannel() != RustChannel.DEFAULT) {
            parameters.add("+" + patched.getChannel());
        } else if (patched.getToolchain() != null) {
            parameters.add("+" + patched.getToolchain());
        }
        if (RsProjectSettingsServiceUtil.getRustSettings(project).getUseOffline()) {
            CargoProject cargoProject = CargoCommandConfiguration.findCargoProject(project, patched.getAdditionalArguments(), patched.getWorkingDirectory());
            SemVer rustcVersion = (cargoProject != null && cargoProject.getRustcInfo() != null && cargoProject.getRustcInfo().getVersion() != null)
                ? cargoProject.getRustcInfo().getVersion().getSemver() : null;
            if (rustcVersion != null) {
                parameters.add("--offline");
            }
        }
        parameters.add(patched.getCommand());
        parameters.addAll(patched.getAdditionalArguments());

        String rustcExecutable = getToolchain().pathToExecutable("rustc").toString();
        return getToolchain().createGeneralCommandLine(
            getExecutable(),
            patched.getWorkingDirectory(),
            patched.getRedirectInputFrom(),
            patched.getBacktraceMode(),
            patched.getEnvironmentVariables(),
            parameters,
            patched.getEmulateTerminal(),
            OpenApiUtil.isFeatureEnabled(RsExperiments.BUILD_TOOL_WINDOW) && patched.getWithSudo(),
            true,
            getHttp()
        ).withEnvironment("RUSTC", rustcExecutable);
    }

    @SuppressWarnings("unchecked")
    private RsResult<ProcessOutput, org.rust.openapiext.RsProcessExecutionException> executeCommandLine(
        CargoCommandLine commandLine,
        Project project,
        @Nullable Disposable owner,
        @Nullable byte[] stdIn,
        @Nullable ProcessListener listener
    ) {
        CargoCommandLine noTerminal = commandLine.withEmulateTerminal(false);
        GeneralCommandLine gcl = toGeneralCommandLine(project, noTerminal);
        Disposable actualOwner = owner != null ? owner : project;
        return CommandLineExt.execute(gcl, actualOwner, stdIn, listener);
    }

    public RsResult<Void, org.rust.openapiext.RsProcessExecutionException.Start> installCargoGenerate(
        Disposable owner, ProcessListener listener
    ) {
        GeneralCommandLine cmd = createBaseCommandLine("install", "cargo-generate");
        RsResult<ProcessOutput, org.rust.openapiext.RsProcessExecutionException> result = CommandLineExt.execute(cmd, owner, null, listener);
        RsResult<ProcessOutput, org.rust.openapiext.RsProcessExecutionException.Start> ignored = RsProcessResultUtil.ignoreExitCode(result);
        if (ignored instanceof RsResult.Err) {
            return new RsResult.Err<>(((RsResult.Err<?, org.rust.openapiext.RsProcessExecutionException.Start>) ignored).err());
        }
        return new RsResult.Ok<>(null);
    }

    public boolean checkNeedInstallCargoGenerate() {
        SemVer minVersion = ToolchainUtil.parseSemVer("0.9.0");
        return checkBinaryCrateIsNotInstalled("cargo-generate", minVersion);
    }

    private boolean checkBinaryCrateIsNotInstalled(String crateName, @Nullable SemVer minVersion) {
        for (BinaryCrate crate : listInstalledBinaryCrates()) {
            if (crate.getName().equals(crateName) &&
                (minVersion == null || (crate.getVersion() != null && crate.getVersion().compareTo(minVersion) >= 0))) {
                return false;
            }
        }
        return true;
    }

    private HttpConfigurable getHttp() {
        return myHttp != null ? myHttp : HttpConfigurable.getInstance();
    }

    @TestOnly
    public void setHttp(HttpConfigurable http) {
        myHttp = http;
    }

    public static final class GeneratedFilesHolder {
        private final VirtualFile myManifest;
        private final List<VirtualFile> mySourceFiles;

        public GeneratedFilesHolder(VirtualFile manifest, List<VirtualFile> sourceFiles) {
            myManifest = manifest;
            mySourceFiles = sourceFiles;
        }

        public VirtualFile getManifest() { return myManifest; }
        public List<VirtualFile> getSourceFiles() { return mySourceFiles; }
    }

    public static CargoPatch getCargoCommonPatch(Project project) {
        return commandLine -> patchArgs(commandLine, project, true);
    }

    public static CargoCommandLine patchArgs(CargoCommandLine commandLine, Project project, boolean colors) {
        List<List<String>> splitResult = commandLine.splitOnDoubleDash();
        List<String> pre = new ArrayList<>(splitResult.get(0));
        List<String> post = new ArrayList<>(splitResult.get(1));

        if (commandLine.getCommand().equals("test") || commandLine.getCommand().equals("bench")) {
            if (commandLine.getAllFeatures() && !pre.contains("--all-features")) {
                pre.add("--all-features");
            }
            if (TEST_NOCAPTURE_ENABLED_KEY.asBoolean() && !post.contains("--nocapture")) {
                post.add(0, "--nocapture");
            }
        }

        if (commandLine.getRequiredFeatures() && FEATURES_ACCEPTING_COMMANDS.contains(commandLine.getCommand())) {
            CargoProject cargoProject = CargoCommandConfiguration.findCargoProject(
                project, commandLine.getAdditionalArguments(), commandLine.getWorkingDirectory()
            );
            if (cargoProject != null) {
                CargoWorkspace.Package cargoPackage = CargoCommandConfiguration.findCargoPackage(
                    cargoProject, commandLine.getAdditionalArguments(), commandLine.getWorkingDirectory()
                );
                if (cargoPackage != null) {
                    if (!commandLine.getWorkingDirectory().equals(cargoPackage.getRootDirectory())) {
                        int manifestIdx = pre.indexOf("--manifest-path");
                        int packageIdx = pre.indexOf("--package");
                        if (manifestIdx == -1 && packageIdx != -1) {
                            pre.remove(packageIdx); // remove --package
                            pre.remove(packageIdx); // remove package name
                            pre.add("--manifest-path");
                            Path manifest = cargoPackage.getRootDirectory().resolve(CargoConstants.MANIFEST_FILE);
                            pre.add(manifest.toAbsolutePath().toString());
                        }
                    }
                    List<CargoWorkspace.Target> cargoTargets = CargoCommandConfiguration.findCargoTargets(
                        cargoPackage, commandLine.getAdditionalArguments()
                    );
                    String features = cargoTargets.stream()
                        .flatMap(t -> t.getRequiredFeatures().stream())
                        .distinct()
                        .collect(Collectors.joining(","));
                    if (!features.isEmpty()) pre.add("--features=" + features);
                }
            }
        }

        // Force colors
        boolean forceColors = colors &&
            COLOR_ACCEPTING_COMMANDS.contains(commandLine.getCommand()) &&
            commandLine.getAdditionalArguments().stream().noneMatch(s -> s.startsWith("--color"));
        if (forceColors) pre.add(0, "--color=always");

        List<String> newArgs = post.isEmpty() ? pre : new ArrayList<>();
        if (!post.isEmpty()) {
            newArgs.addAll(pre);
            newArgs.add("--");
            newArgs.addAll(post);
        }
        return commandLine.withAdditionalArguments(newArgs);
    }

    public static boolean checkNeedInstallGrcov(Project project) {
        String crateName = RsBundle.message("notification.content.grcov");
        SemVer minVersion = ToolchainUtil.parseSemVer("0.7.0");
        return checkNeedInstallBinaryCrate(project, crateName, NotificationType.ERROR,
            RsBundle.message("notification.content.need.at.least4", crateName, minVersion), minVersion);
    }

    public static boolean checkNeedInstallCargoExpand(Project project) {
        String crateName = RsBundle.message("notification.content.cargo.expand");
        SemVer minVersion = ToolchainUtil.parseSemVer("1.0.0");
        return checkNeedInstallBinaryCrate(project, crateName, NotificationType.ERROR,
            RsBundle.message("notification.content.need.at.least3", crateName, minVersion), minVersion);
    }

    public static boolean checkNeedInstallEvcxr(Project project) {
        String crateName = "evcxr_repl";
        SemVer minVersion = ToolchainUtil.parseSemVer("0.14.2");
        return checkNeedInstallBinaryCrate(project, crateName, NotificationType.ERROR,
            RsBundle.message("notification.content.need.at.least2", crateName, minVersion), minVersion);
    }

    public static boolean checkNeedInstallWasmPack(Project project) {
        String crateName = RsBundle.message("notification.content.wasm.pack");
        SemVer minVersion = ToolchainUtil.parseSemVer("0.9.1");
        return checkNeedInstallBinaryCrate(project, crateName, NotificationType.ERROR,
            RsBundle.message("notification.content.need.at.least", crateName, minVersion), minVersion);
    }

    private static boolean checkNeedInstallBinaryCrate(
        Project project,
        String crateName,
        NotificationType notificationType,
        @Nullable String message,
        @Nullable SemVer minVersion
    ) {
        RsToolchainBase toolchain = RsProjectSettingsServiceUtil.getToolchain(project);
        if (toolchain == null) return false;
        Cargo cargo = CargoExtUtil.cargo(toolchain);
        java.util.function.Supplier<Boolean> isNotInstalled = () -> cargo.checkBinaryCrateIsNotInstalled(crateName, minVersion);
        boolean needInstall;
        if (OpenApiUtil.isDispatchThread()) {
            needInstall = OpenApiUtil.computeWithCancelableProgress(project,
                RsBundle.message("progress.title.checking.if.installed", crateName), isNotInstalled);
        } else {
            needInstall = isNotInstalled.get();
        }

        if (needInstall) {
            NotificationUtils.showBalloon(
                project,
                RsBundle.message("notification.title.code.code.not.installed", crateName),
                message != null ? message : "",
                notificationType,
                new InstallBinaryCrateAction(crateName),
                null
            );
        }

        return needInstall;
    }
}
