/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig;

import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExternalizablePath;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.filters.Filter;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.RunContentManager;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.project.model.CargoProjectsService;
import org.rust.cargo.project.settings.RustProjectSettingsService;
import org.rust.cargo.project.toolwindow.CargoToolWindow;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;
import org.rust.cargo.runconfig.command.CargoCommandConfigurationType;
import org.rust.cargo.runconfig.filters.*;
import org.rust.cargo.runconfig.target.TargetUtil;
import org.rust.cargo.toolchain.CargoCommandLine;
import org.rust.cargo.toolchain.tools.Cargo;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class RunConfigUtil {

    private RunConfigUtil() {
    }

    @NotNull
    public static CargoCommandLine mergeWithDefault(@NotNull CargoCommandLine commandLine, @NotNull CargoCommandConfiguration defaultConfig) {
        return commandLine.copy(
            commandLine.getCommand(),
            commandLine.getWorkingDirectory(),
            commandLine.getAdditionalArguments(),
            commandLine.getRedirectInputFrom(),
            defaultConfig.getEmulateTerminal(),
            defaultConfig.getBacktrace(),
            commandLine.getToolchain(),
            defaultConfig.getChannel(),
            defaultConfig.getEnv(),
            defaultConfig.getRequiredFeatures(),
            defaultConfig.getAllFeatures(),
            defaultConfig.getWithSudo()
        );
    }

    @NotNull
    public static RunnerAndConfigurationSettings createCargoCommandRunConfiguration(
        @NotNull RunManager runManager,
        @NotNull CargoCommandLine cargoCommandLine,
        @Nullable String name
    ) {
        RunnerAndConfigurationSettings runnerAndConfigurationSettings = runManager.createConfiguration(
            name != null ? name : cargoCommandLine.getCommand(),
            CargoCommandConfigurationType.getInstance().getFactory()
        );
        CargoCommandConfiguration configuration = (CargoCommandConfiguration) runnerAndConfigurationSettings.getConfiguration();
        configuration.setFromCmd(cargoCommandLine);
        return runnerAndConfigurationSettings;
    }

    @NotNull
    public static RunnerAndConfigurationSettings createCargoCommandRunConfiguration(
        @NotNull RunManager runManager,
        @NotNull CargoCommandLine cargoCommandLine
    ) {
        return createCargoCommandRunConfiguration(runManager, cargoCommandLine, null);
    }

    public static boolean hasCargoProject(@NotNull Project project) {
        return !CargoProjectServiceUtil.getCargoProjects(project).getAllProjects().isEmpty();
    }

    public static void buildProject(@NotNull Project project) {
        org.rust.openapiext.OpenApiUtil.checkIsDispatchThread();
        List<String> arguments = new ArrayList<>();
        RustProjectSettingsService settings = project.getService(RustProjectSettingsService.class);
        arguments.add("--all");
        if (settings.getCompileAllTargets()) {
            org.rust.cargo.toolchain.RsToolchainBase toolchain = settings.getToolchain();
            boolean allTargets = false;
            if (toolchain != null) {
                allTargets = new Cargo(toolchain).checkSupportForBuildCheckAllTargets();
            }
            if (allTargets) {
                arguments.add("--all-targets");
            }
        }

        // Initialize run content manager
        RunContentManager.getInstance(project);

        for (CargoProject cargoProject : CargoProjectServiceUtil.getCargoProjects(project).getAllProjects()) {
            CargoCommandLine.forProject(cargoProject, "build", arguments)
                .run(cargoProject, "build", false);
        }
    }

    public static void cleanProject(@NotNull Project project) {
        org.rust.openapiext.OpenApiUtil.checkIsDispatchThread();

        // Initialize run content manager
        RunContentManager.getInstance(project);

        for (CargoProject cargoProject : CargoProjectServiceUtil.getCargoProjects(project).getAllProjects()) {
            CargoCommandLine.forProject(cargoProject, "clean", java.util.Collections.emptyList(), false, null,
                    org.rust.cargo.toolchain.RustChannel.DEFAULT,
                    com.intellij.execution.configuration.EnvironmentVariablesData.DEFAULT)
                .run(cargoProject, "clean", false);
        }
    }

    @Nullable
    public static CargoProject getAppropriateCargoProject(@NotNull DataContext dataContext) {
        Project project = dataContext.getData(CommonDataKeys.PROJECT);
        if (project == null) return null;
        CargoProjectsService cargoProjects = CargoProjectServiceUtil.getCargoProjects(project);
        Collection<CargoProject> allProjects = cargoProjects.getAllProjects();
        if (allProjects.size() == 1) {
            return allProjects.iterator().next();
        }

        VirtualFile file = dataContext.getData(CommonDataKeys.VIRTUAL_FILE);
        if (file != null) {
            CargoProject found = cargoProjects.findProjectForFile(file);
            if (found != null) return found;
        }

        CargoProject selected = dataContext.getData(CargoToolWindow.SELECTED_CARGO_PROJECT);
        if (selected != null) return selected;

        return allProjects.isEmpty() ? null : allProjects.iterator().next();
    }

    @NotNull
    public static Collection<Filter> createFilters(@Nullable CargoProject cargoProject) {
        List<Filter> filters = new ArrayList<>();
        filters.add(new RsExplainFilter());
        if (cargoProject != null) {
            VirtualFile dir = cargoProject.getWorkspaceRootDir();
            if (dir == null) dir = cargoProject.getRootDir();
            if (dir != null) {
                filters.add(new RsConsoleFilter(cargoProject.getProject(), dir));
                filters.add(new RsDbgFilter(cargoProject.getProject(), dir));
                filters.add(new RsPanicFilter(cargoProject.getProject(), dir));
                filters.add(new RsBacktraceFilter(cargoProject.getProject(), dir, cargoProject.getWorkspace()));
            }
        }
        return filters;
    }

    public static void addFormatJsonOption(@NotNull List<String> additionalArguments, @NotNull String formatOption, @NotNull String format) {
        String formatJsonOption = formatOption + "=" + format;
        int idx = additionalArguments.indexOf(formatOption);
        int indexArgWithValue = -1;
        for (int i = 0; i < additionalArguments.size(); i++) {
            if (additionalArguments.get(i).startsWith(formatOption)) {
                indexArgWithValue = i;
                break;
            }
        }
        if (idx != -1) {
            if (idx < additionalArguments.size() - 1) {
                if (!additionalArguments.get(idx + 1).startsWith("-")) {
                    additionalArguments.set(idx + 1, format);
                } else {
                    additionalArguments.add(idx + 1, format);
                }
            } else {
                additionalArguments.add(format);
            }
        } else if (indexArgWithValue != -1) {
            additionalArguments.set(indexArgWithValue, formatJsonOption);
        } else {
            additionalArguments.add(0, formatJsonOption);
        }
    }

    public static void writeString(@NotNull Element element, @NotNull String name, @NotNull String value) {
        Element opt = new Element("option");
        opt.setAttribute("name", name);
        opt.setAttribute("value", value);
        element.addContent(opt);
    }

    @Nullable
    public static String readString(@NotNull Element element, @NotNull String name) {
        for (Element child : element.getChildren()) {
            if ("option".equals(child.getName()) && name.equals(child.getAttributeValue("name"))) {
                return child.getAttributeValue("value");
            }
        }
        return null;
    }

    public static void writeBool(@NotNull Element element, @NotNull String name, boolean value) {
        writeString(element, name, String.valueOf(value));
    }

    @Nullable
    public static Boolean readBool(@NotNull Element element, @NotNull String name) {
        String s = readString(element, name);
        return s != null ? Boolean.valueOf(s) : null;
    }

    public static <E extends Enum<?>> void writeEnum(@NotNull Element element, @NotNull String name, @NotNull E value) {
        writeString(element, name, value.name());
    }

    @Nullable
    public static <E extends Enum<E>> E readEnum(@NotNull Element element, @NotNull String name, @NotNull Class<E> enumClass) {
        String variantName = readString(element, name);
        if (variantName == null) return null;
        try {
            return Enum.valueOf(enumClass, variantName);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static void writePath(@NotNull Element element, @NotNull String name, @Nullable Path value) {
        if (value != null) {
            String s = ExternalizablePath.urlValue(value.toString());
            writeString(element, name, s);
        }
    }

    @Nullable
    public static Path readPath(@NotNull Element element, @NotNull String name) {
        String s = readString(element, name);
        if (s == null) return null;
        return Paths.get(ExternalizablePath.localPathValue(s));
    }

    @NotNull
    public static DefaultExecutionResult executeCommandLine(
        @NotNull CargoRunStateBase state,
        @NotNull GeneralCommandLine commandLine,
        @NotNull ExecutionEnvironment environment
    ) throws com.intellij.execution.ExecutionException {
        CargoCommandConfiguration runConfiguration = (CargoCommandConfiguration) state.getRunConfiguration();
        com.intellij.execution.target.TargetEnvironmentConfiguration targetEnvironment =
            TargetUtil.getTargetEnvironment(runConfiguration);
        ConfigurationExtensionContext context = new ConfigurationExtensionContext();

        RsRunConfigurationExtensionManager extensionManager = RsRunConfigurationExtensionManager.getInstance();
        extensionManager.patchCommandLine(runConfiguration, environment, commandLine, context);
        extensionManager.patchCommandLineState(runConfiguration, environment, state, context);
        com.intellij.execution.process.ProcessHandler handler = TargetUtil.startProcess(
            commandLine, environment.getProject(), targetEnvironment, true, true);
        extensionManager.attachExtensionsToProcess(runConfiguration, handler, environment, context);

        com.intellij.execution.ui.ConsoleView console = state.getConsoleBuilder().getConsole();
        console.attachToProcess(handler);
        return new DefaultExecutionResult(console, handler);
    }

    /**
     * Checks whether the given configuration has a remote target.
     */
    public static boolean hasRemoteTarget(@NotNull RsCommandConfiguration config) {
        if (config instanceof org.rust.cargo.runconfig.command.CargoCommandConfiguration) {
            return TargetUtil.hasRemoteTarget((org.rust.cargo.runconfig.command.CargoCommandConfiguration) config);
        }
        return false;
    }

    // Convenience getters for backward compatibility

    public static boolean getHasCargoProject(@NotNull Project project) {
        return hasCargoProject(project);
    }

    public static boolean getHasRemoteTarget(@NotNull RsCommandConfiguration config) {
        return hasRemoteTarget(config);
    }
}
