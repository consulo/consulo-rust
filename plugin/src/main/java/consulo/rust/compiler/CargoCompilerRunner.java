/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package consulo.rust.compiler;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import consulo.annotation.component.ExtensionImpl;
import consulo.build.ui.FilePosition;
import consulo.build.ui.event.MessageEvent;
import consulo.build.ui.progress.BuildProgress;
import consulo.build.ui.progress.BuildProgressDescriptor;
import consulo.compiler.CompileContextEx;
import consulo.compiler.CompileDriver;
import consulo.compiler.CompilerRunner;
import consulo.compiler.scope.CompileScope;
import consulo.dataContext.DataContext;
import consulo.module.extension.ModuleExtensionHelper;
import jakarta.inject.Inject;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.process.ExecutionException;
import consulo.process.ProcessHandler;
import consulo.process.ProcessHandlerBuilder;
import consulo.process.ProcessOutputTypes;
import consulo.process.cmd.GeneralCommandLine;
import consulo.process.event.ProcessEvent;
import consulo.process.event.ProcessListener;
import consulo.project.Project;
import consulo.rust.module.extension.RustModuleExtension;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nullable;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;
import org.rust.cargo.runconfig.wasmpack.WasmPackCommandConfiguration;
import org.rust.cargo.toolchain.tools.Rustup;
import org.rust.cargo.util.CargoArgsParserUtil;
import consulo.process.cmd.ParametersListUtil;
import org.rust.cargo.toolchain.CargoCommandLine;
import org.rust.cargo.toolchain.RsToolchainBase;
import org.rust.cargo.api.toolchain.RustcMessage.CargoTopMessage;
import org.rust.cargo.api.toolchain.RustcMessage.CompilerArtifactMessage;
import org.rust.cargo.api.toolchain.RustcMessage.RustcDiagnostic;
import org.rust.cargo.api.toolchain.RustcMessage.RustcSpan;
import org.rust.cargo.toolchain.tools.Cargo;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.rust.cargo.api.model.CargoProject;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.runconfig.RunConfigUtil;

/**
 * Builds a Rust module with cargo, driven by the platform compiler rather than by a private
 * build session of our own: the platform owns the progress UI, the cancellation and the
 * build tool window, and this runner only produces the command line and translates cargo's
 * JSON output into build events.
 * <p>
 * The toolchain always comes from the module's {@link RustModuleExtension}, so a module with no
 * Rust bundle selected is reported as a configuration error instead of silently falling back to
 * whatever cargo happens to be on the PATH.
 */
@ExtensionImpl
public class CargoCompilerRunner implements CompilerRunner {
    private static final Logger LOG = Logger.getInstance(CargoCompilerRunner.class);

    /**
     * Artifacts cargo reported during the build, published on the {@link ExecutionEnvironment}
     * so the program runner can locate the executable it has to launch.
     */
    public static final Key<List<CompilerArtifactMessage>> ARTIFACTS = Key.create("rust.cargo.artifacts");

    private static final List<String> TEST_COMMANDS = List.of("test", "bench");

    private final Project myProject;
    private final ModuleExtensionHelper myModuleExtensionHelper;

    @Inject
    public CargoCompilerRunner(Project project, ModuleExtensionHelper moduleExtensionHelper) {
        myProject = project;
        myModuleExtensionHelper = moduleExtensionHelper;
    }

    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("Cargo");
    }

    @Override
    public Result checkAvailable(DataContext dataContext) {
        if (!myModuleExtensionHelper.hasModuleExtension(RustModuleExtension.class)) {
            return NO;
        }

        // The data context here is a view over the compile scope, so a run configuration is present
        // only when the build was started to launch something. Anything that is not ours is left to
        // the runner that owns it.
        RunConfiguration runConfiguration = dataContext.getData(RunConfiguration.KEY);
        if (runConfiguration != null
            && !(runConfiguration instanceof CargoCommandConfiguration)
            && !(runConfiguration instanceof WasmPackCommandConfiguration)) {
            return NO;
        }

        return new YesResult(PlatformIconGroup.actionsCompile());
    }

    @Override
    public boolean build(
        CompileDriver compileDriver,
        CompileContextEx context,
        BuildProgress<BuildProgressDescriptor> buildProgress,
        boolean isRebuild,
        boolean forceCompile,
        boolean onlyCheckStatus
    ) {
        if (onlyCheckStatus) {
            // Cargo decides for itself what is out of date, so there is nothing to answer here.
            return false;
        }

        Project project = myProject;
        CompileScope compileScope = context.getCompileScope();
        RunConfiguration runConfiguration = compileScope.getUserData(RunConfiguration.KEY);

        Module module = findRustModule(project);
        if (module == null) {
            context.newError(LocalizeValue.localizeTODO("No module with a Rust toolchain bundle")).add();
            return false;
        }

        RsToolchainBase toolchain = RustModuleExtension.findToolchain(module);
        if (toolchain == null) {
            context.newError(LocalizeValue.localizeTODO(
                "Module '" + module.getName() + "' has no Rust toolchain bundle selected")).add();
            return false;
        }

        if (runConfiguration instanceof WasmPackCommandConfiguration wasm) {
            Path workingDirectory = wasm.getWorkingDirectory();
            if (workingDirectory == null) {
                context.newError(LocalizeValue.localizeTODO("No working directory set for '" + wasm.getName() + "'")).add();
                return false;
            }
            if (Rustup.checkNeedInstallWasmTarget(project, workingDirectory)) {
                context.newError(LocalizeValue.localizeTODO(
                    "The " + Rustup.WASM_TARGET + " target is not installed for this toolchain")).add();
                return false;
            }
        }

        CargoCommandLine commandLine = buildCommandLine(runConfiguration, project, module);
        if (commandLine == null) {
            context.newError(LocalizeValue.localizeTODO("No cargo project found for module '" + module.getName() + "'")).add();
            return false;
        }

        GeneralCommandLine generalCommandLine =
            Cargo.cargoOrWrapper(toolchain, commandLine.getWorkingDirectory()).toGeneralCommandLine(project, commandLine);

        context.getProgressIndicator().setText("cargo " + commandLine.getCommand());

        List<CompilerArtifactMessage> artifacts = new CopyOnWriteArrayList<>();
        int exitCode = run(generalCommandLine, context, buildProgress, artifacts);

        ExecutionEnvironment environment = compileScope.getUserData(ExecutionEnvironment.KEY);
        if (environment != null) {
            environment.putUserData(ARTIFACTS, List.copyOf(artifacts));
        }

        return exitCode == 0;
    }

    private int run(
        GeneralCommandLine generalCommandLine,
        CompileContextEx context,
        BuildProgress<BuildProgressDescriptor> buildProgress,
        List<CompilerArtifactMessage> artifacts
    ) {
        ProcessHandler processHandler;
        try {
            processHandler = ProcessHandlerBuilder.create(generalCommandLine).killable().build();
        }
        catch (ExecutionException e) {
            context.newError(LocalizeValue.ofNullable(e.getMessage())).add();
            return -1;
        }

        int[] exitCode = {-1};
        processHandler.addProcessListener(new ProcessListener() {
            @Override
            public void onTextAvailable(ProcessEvent event, Key outputType) {
                String text = event.getText();
                if (outputType == ProcessOutputTypes.STDOUT) {
                    // Only stdout carries the JSON stream; anything unparseable there is still
                    // worth showing, so it falls through to the plain output below.
                    if (consume(text, buildProgress, artifacts)) {
                        return;
                    }
                }
                buildProgress.output(text, outputType != ProcessOutputTypes.STDERR);
            }

            @Override
            public void processTerminated(ProcessEvent event) {
                exitCode[0] = event.getExitCode();
            }
        });

        processHandler.startNotify();
        while (!processHandler.waitFor(100)) {
            if (context.getProgressIndicator().isCanceled()) {
                processHandler.destroyProcess();
                return -1;
            }
        }
        return exitCode[0];
    }

    private static boolean consume(
        String line,
        BuildProgress<BuildProgressDescriptor> buildProgress,
        List<CompilerArtifactMessage> artifacts
    ) {
        String trimmed = line.trim();
        if (!trimmed.startsWith("{")) {
            return false;
        }

        JsonObject json;
        try {
            json = JsonParser.parseString(trimmed).getAsJsonObject();
        }
        catch (JsonSyntaxException | IllegalStateException e) {
            return false;
        }

        try {
            CargoTopMessage topMessage = CargoTopMessage.fromJson(json);
            if (topMessage != null) {
                report(topMessage.getMessage(), buildProgress);
                return true;
            }

            CompilerArtifactMessage artifact = CompilerArtifactMessage.fromJson(json);
            if (artifact != null) {
                artifacts.add(artifact);
                return true;
            }
        }
        catch (Exception e) {
            LOG.warn("Failed to read cargo message: " + trimmed, e);
        }

        // A recognised JSON message with nothing to show - build-script output, progress, and so on.
        return true;
    }

    private static void report(RustcDiagnostic diagnostic, BuildProgress<BuildProgressDescriptor> buildProgress) {
        MessageEvent.Kind kind = kindOf(diagnostic.getLevel());
        if (kind == null) {
            return;
        }

        String rendered = diagnostic.getRendered();
        LocalizeValue message = LocalizeValue.of(rendered != null ? rendered : diagnostic.getMessage());
        LocalizeValue title = LocalizeValue.of(diagnostic.getMessage());

        RustcSpan span = diagnostic.getMainSpan();
        if (span == null) {
            buildProgress.message(title, message, kind, null);
            return;
        }

        buildProgress.fileMessage(
            title,
            message,
            kind,
            new FilePosition(
                new File(span.getFile_name()),
                span.getLine_start() - 1,
                span.getColumn_start() - 1,
                span.getLine_end() - 1,
                span.getColumn_end() - 1
            )
        );
    }

    @Nullable
    private static MessageEvent.Kind kindOf(String level) {
        return switch (level) {
            case "error", "error: internal compiler error" -> MessageEvent.Kind.ERROR;
            case "warning" -> MessageEvent.Kind.WARNING;
            case "note", "help" -> MessageEvent.Kind.INFO;
            default -> null;
        };
    }

    /**
     * The command that produces the binaries without running them: {@code run} becomes a plain
     * {@code build}, while {@code test} and {@code bench} keep their own command because only they
     * know which test harness to compile, and are stopped from executing with {@code --no-run}.
     */
    @Nullable
    private static CargoCommandLine buildCommandLine(
        @Nullable RunConfiguration runConfiguration,
        Project project,
        Module module
    ) {
        CargoCommandLine base = null;
        if (runConfiguration instanceof CargoCommandConfiguration configuration) {
            CargoCommandConfiguration.CleanConfiguration.Ok clean = configuration.clean().getOk();
            if (clean != null) {
                base = clean.getCmd();
            }
        }

        if (base == null && runConfiguration instanceof WasmPackCommandConfiguration wasm) {
            base = wasmPackBuildCommandLine(wasm);
        }

        if (base == null) {
            Path workingDirectory = workingDirectoryOf(project, module);
            if (workingDirectory == null) {
                return null;
            }
            base = new CargoCommandLine("build", workingDirectory);
        }

        List<String> arguments = new ArrayList<>(base.getAdditionalArguments());
        arguments.remove("-q");
        arguments.remove("--quiet");
        RunConfigUtil.addFormatJsonOption(arguments, "--message-format", "json");

        String command = base.getCommand();
        if (TEST_COMMANDS.contains(command)) {
            if (!arguments.contains("--no-run")) {
                arguments.add(0, "--no-run");
            }
        }
        else {
            command = "build";
        }

        return base.copy(
            command,
            base.getWorkingDirectory(),
            arguments,
            base.getRedirectInputFrom(),
            false,
            base.getBacktraceMode(),
            base.getToolchain(),
            base.getChannel(),
            base.getEnvironmentVariables(),
            base.getRequiredFeatures(),
            base.getAllFeatures(),
            false
        );
    }

    /**
     * wasm-pack drives cargo itself, but the IDE still builds first so that diagnostics reach the
     * build view. The target and profile have to match what wasm-pack would have used, or the build
     * lands in a different directory and warms nothing.
     */
    @Nullable
    private static CargoCommandLine wasmPackBuildCommandLine(WasmPackCommandConfiguration wasm) {
        Path workingDirectory = wasm.getWorkingDirectory();
        if (workingDirectory == null) {
            return null;
        }

        List<String> configurationArgs = ParametersListUtil.parse(wasm.getCommand());
        if (configurationArgs.isEmpty()) {
            return null;
        }

        List<List<String>> split = CargoArgsParserUtil.splitOnDoubleDash(configurationArgs);
        List<String> preArgs = split.get(0);
        List<String> postArgs = split.get(1);

        List<String> arguments = new ArrayList<>(postArgs);
        if ("test".equals(configurationArgs.get(0))) {
            arguments.add("--tests");
        }
        if (!preArgs.contains("--dev")) {
            arguments.add("--release");
        }
        if (!postArgs.contains("--target")) {
            arguments.add("--target");
            arguments.add(Rustup.WASM_TARGET);
        }

        return new CargoCommandLine("build", workingDirectory, arguments);
    }

    @Nullable
    private static Path workingDirectoryOf(Project project, Module module) {
        for (CargoProject cargoProject :
            CargoProjectServiceUtil.getCargoProjects(project).getAllProjects()) {
            Path manifest = cargoProject.getManifest();
            if (manifest != null && manifest.getParent() != null) {
                return manifest.getParent();
            }
        }
        return null;
    }

    /**
     * The first module carrying the Rust extension. Cargo drives the whole workspace from one
     * manifest, so a per-module walk would only ever re-run the same build.
     */
    @Nullable
    private static Module findRustModule(Project project) {
        for (Module module : ModuleManager.getInstance(project).getModules()) {
            if (RustModuleExtension.findExtension(module) != null) {
                return module;
            }
        }
        return null;
    }
}
