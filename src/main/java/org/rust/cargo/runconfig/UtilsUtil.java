/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig;

import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.filters.Filter;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;
import org.rust.cargo.toolchain.CargoCommandLine;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

/**
 * Bridge class delegating to {@link RunConfigUtil}.
 */
public final class UtilsUtil {

    private UtilsUtil() {
    }

    @NotNull
    public static CargoCommandLine mergeWithDefault(@NotNull CargoCommandLine commandLine, @NotNull CargoCommandConfiguration defaultConfig) {
        return RunConfigUtil.mergeWithDefault(commandLine, defaultConfig);
    }

    @NotNull
    public static RunnerAndConfigurationSettings createCargoCommandRunConfiguration(
        @NotNull RunManager runManager,
        @NotNull CargoCommandLine cargoCommandLine,
        @Nullable String name
    ) {
        return RunConfigUtil.createCargoCommandRunConfiguration(runManager, cargoCommandLine, name);
    }

    @NotNull
    public static RunnerAndConfigurationSettings createCargoCommandRunConfiguration(
        @NotNull RunManager runManager,
        @NotNull CargoCommandLine cargoCommandLine
    ) {
        return RunConfigUtil.createCargoCommandRunConfiguration(runManager, cargoCommandLine);
    }

    public static boolean getHasCargoProject(@NotNull Project project) {
        return RunConfigUtil.hasCargoProject(project);
    }

    public static boolean hasCargoProject(@NotNull Project project) {
        return RunConfigUtil.hasCargoProject(project);
    }

    public static void buildProject(@NotNull Project project) {
        RunConfigUtil.buildProject(project);
    }

    public static void cleanProject(@NotNull Project project) {
        RunConfigUtil.cleanProject(project);
    }

    @Nullable
    public static CargoProject getAppropriateCargoProject(@NotNull DataContext dataContext) {
        return RunConfigUtil.getAppropriateCargoProject(dataContext);
    }

    @NotNull
    public static Collection<Filter> createFilters(@Nullable CargoProject cargoProject) {
        return RunConfigUtil.createFilters(cargoProject);
    }

    public static void addFormatJsonOption(@NotNull List<String> additionalArguments, @NotNull String formatOption, @NotNull String format) {
        RunConfigUtil.addFormatJsonOption(additionalArguments, formatOption, format);
    }

    public static void writeString(@NotNull Element element, @NotNull String name, @NotNull String value) {
        RunConfigUtil.writeString(element, name, value);
    }

    @Nullable
    public static String readString(@NotNull Element element, @NotNull String name) {
        return RunConfigUtil.readString(element, name);
    }

    public static void writeBool(@NotNull Element element, @NotNull String name, boolean value) {
        RunConfigUtil.writeBool(element, name, value);
    }

    @Nullable
    public static Boolean readBool(@NotNull Element element, @NotNull String name) {
        return RunConfigUtil.readBool(element, name);
    }

    public static <E extends Enum<?>> void writeEnum(@NotNull Element element, @NotNull String name, @NotNull E value) {
        RunConfigUtil.writeEnum(element, name, value);
    }

    @Nullable
    public static <E extends Enum<E>> E readEnum(@NotNull Element element, @NotNull String name, @NotNull Class<E> enumClass) {
        return RunConfigUtil.readEnum(element, name, enumClass);
    }

    public static void writePath(@NotNull Element element, @NotNull String name, @Nullable Path value) {
        RunConfigUtil.writePath(element, name, value);
    }

    @Nullable
    public static Path readPath(@NotNull Element element, @NotNull String name) {
        return RunConfigUtil.readPath(element, name);
    }

    @NotNull
    public static DefaultExecutionResult executeCommandLine(
        @NotNull CargoRunStateBase state,
        @NotNull GeneralCommandLine commandLine,
        @NotNull ExecutionEnvironment environment
    ) throws com.intellij.execution.ExecutionException {
        return RunConfigUtil.executeCommandLine(state, commandLine, environment);
    }

    public static boolean getHasRemoteTarget(@NotNull RsCommandConfiguration config) {
        return RunConfigUtil.getHasRemoteTarget(config);
    }
}
