/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig;

import consulo.execution.DefaultExecutionResult;
import consulo.execution.RunManager;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.process.cmd.GeneralCommandLine;
import consulo.execution.ui.console.Filter;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.dataContext.DataContext;
import consulo.project.Project;
import org.jdom.Element;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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

    @Nonnull
    public static CargoCommandLine mergeWithDefault(@Nonnull CargoCommandLine commandLine, @Nonnull CargoCommandConfiguration defaultConfig) {
        return RunConfigUtil.mergeWithDefault(commandLine, defaultConfig);
    }

    @Nonnull
    public static RunnerAndConfigurationSettings createCargoCommandRunConfiguration(
        @Nonnull RunManager runManager,
        @Nonnull CargoCommandLine cargoCommandLine,
        @Nullable String name
    ) {
        return RunConfigUtil.createCargoCommandRunConfiguration(runManager, cargoCommandLine, name);
    }

    @Nonnull
    public static RunnerAndConfigurationSettings createCargoCommandRunConfiguration(
        @Nonnull RunManager runManager,
        @Nonnull CargoCommandLine cargoCommandLine
    ) {
        return RunConfigUtil.createCargoCommandRunConfiguration(runManager, cargoCommandLine);
    }

    public static boolean getHasCargoProject(@Nonnull Project project) {
        return RunConfigUtil.hasCargoProject(project);
    }

    public static boolean hasCargoProject(@Nonnull Project project) {
        return RunConfigUtil.hasCargoProject(project);
    }

    public static void buildProject(@Nonnull Project project) {
        RunConfigUtil.buildProject(project);
    }

    public static void cleanProject(@Nonnull Project project) {
        RunConfigUtil.cleanProject(project);
    }

    @Nullable
    public static CargoProject getAppropriateCargoProject(@Nonnull DataContext dataContext) {
        return RunConfigUtil.getAppropriateCargoProject(dataContext);
    }

    @Nonnull
    public static Collection<Filter> createFilters(@Nullable CargoProject cargoProject) {
        return RunConfigUtil.createFilters(cargoProject);
    }

    public static void addFormatJsonOption(@Nonnull List<String> additionalArguments, @Nonnull String formatOption, @Nonnull String format) {
        RunConfigUtil.addFormatJsonOption(additionalArguments, formatOption, format);
    }

    public static void writeString(@Nonnull Element element, @Nonnull String name, @Nonnull String value) {
        RunConfigUtil.writeString(element, name, value);
    }

    @Nullable
    public static String readString(@Nonnull Element element, @Nonnull String name) {
        return RunConfigUtil.readString(element, name);
    }

    public static void writeBool(@Nonnull Element element, @Nonnull String name, boolean value) {
        RunConfigUtil.writeBool(element, name, value);
    }

    @Nullable
    public static Boolean readBool(@Nonnull Element element, @Nonnull String name) {
        return RunConfigUtil.readBool(element, name);
    }

    public static <E extends Enum<?>> void writeEnum(@Nonnull Element element, @Nonnull String name, @Nonnull E value) {
        RunConfigUtil.writeEnum(element, name, value);
    }

    @Nullable
    public static <E extends Enum<E>> E readEnum(@Nonnull Element element, @Nonnull String name, @Nonnull Class<E> enumClass) {
        return RunConfigUtil.readEnum(element, name, enumClass);
    }

    public static void writePath(@Nonnull Element element, @Nonnull String name, @Nullable Path value) {
        RunConfigUtil.writePath(element, name, value);
    }

    @Nullable
    public static Path readPath(@Nonnull Element element, @Nonnull String name) {
        return RunConfigUtil.readPath(element, name);
    }

    @Nonnull
    public static DefaultExecutionResult executeCommandLine(
        @Nonnull CargoRunStateBase state,
        @Nonnull GeneralCommandLine commandLine,
        @Nonnull ExecutionEnvironment environment
    ) throws consulo.process.ExecutionException {
        return RunConfigUtil.executeCommandLine(state, commandLine, environment);
    }

    public static boolean getHasRemoteTarget(@Nonnull RsCommandConfiguration config) {
        return RunConfigUtil.getHasRemoteTarget(config);
    }
}
