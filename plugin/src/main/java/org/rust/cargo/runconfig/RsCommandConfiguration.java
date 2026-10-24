/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig;

import com.intellij.execution.configurations.RunConfigurationWithSuppressedDefaultDebugAction;
import consulo.execution.configuration.ConfigurationFactory;
import consulo.execution.configuration.ExternalizablePath;
import consulo.execution.configuration.LocatableConfigurationBase;
import consulo.execution.configuration.RunProfileWithCompileBeforeLaunchOption;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.project.Project;
import consulo.rust.module.extension.RustModuleExtension;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.api.model.CargoProject;
import org.rust.cargo.api.model.CargoProjectsService;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.experiments.RsExperiments;
import org.rust.openapiext.OpenApiUtil;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

public abstract class RsCommandConfiguration extends LocatableConfigurationBase
    implements RunConfigurationWithSuppressedDefaultDebugAction,
    RunProfileWithCompileBeforeLaunchOption {
    /**
     * The Rust modules of the project. The platform builds the compile scope from these before
     * launching, which is what routes the build through the toolchain on the module's Rust extension.
     */
    @Nonnull
    @Override
    public Module[] getModules() {
        java.util.List<Module> modules = new java.util.ArrayList<>();
        for (Module module : ModuleManager.getInstance(getProject()).getModules()) {
            if (RustModuleExtension.findExtension(module) != null) {
                modules.add(module);
            }
        }
        return modules.toArray(Module.EMPTY_ARRAY);
    }

    /**
     * Without a Rust module there is nothing cargo could build, so an empty module list must not
     * fall back to compiling the whole project.
     */
    @Override
    public boolean isBuildProjectOnEmptyModuleList() {
        return false;
    }

    protected RsCommandConfiguration(@Nonnull Project project, @Nonnull String name, @Nonnull ConfigurationFactory factory) {
        super(project, factory, name);
        if (!project.isDefault()) {
            CargoProjectsService cargoProjects = CargoProjectServiceUtil.getCargoProjects(project);
            Collection<CargoProject> allProjects = cargoProjects.getAllProjects();
            if (!allProjects.isEmpty()) {
                setWorkingDirectory(org.rust.cargo.project.model.CargoProjectLocator.getWorkingDirectory(allProjects.iterator().next()));
            }
        }
    }

    @Override
    public RsCommandConfigurationOptions getOptions() {
        return (RsCommandConfigurationOptions) super.getOptions();
    }

    /**
     * Converts the command, the working directory and the terminal emulation of an old configuration element.
     */
    protected static void convertLegacyCommandState(@Nonnull RsCommandConfigurationLegacyState old,
                                                    @Nonnull RsCommandConfigurationOptions options) {
        if (old.command != null) {
            options.setCommand(old.command);
        }
        if (old.workingDirectory != null) {
            options.setWorkingDirectory(FileUtil.toSystemIndependentName(ExternalizablePath.localPathValue(old.workingDirectory)));
        }
        if (old.emulateTerminal != null) {
            options.setEmulateTerminal(Boolean.valueOf(old.emulateTerminal));
        }
    }

    @Nonnull
    public String getCommand() {
        return StringUtil.notNullize(getOptions().getCommand());
    }

    public void setCommand(@Nonnull String command) {
        getOptions().setCommand(command);
    }

    public boolean getEmulateTerminal() {
        Boolean emulateTerminal = getOptions().getEmulateTerminal();
        return emulateTerminal != null ? emulateTerminal : getEmulateTerminalDefault();
    }

    public void setEmulateTerminal(boolean emulateTerminal) {
        getOptions().setEmulateTerminal(emulateTerminal == getEmulateTerminalDefault() ? null : emulateTerminal);
    }

    @Nullable
    public Path getWorkingDirectory() {
        String workingDirectory = getOptions().getWorkingDirectory();
        return workingDirectory == null ? null : Paths.get(FileUtil.toSystemDependentName(workingDirectory));
    }

    public void setWorkingDirectory(@Nullable Path workingDirectory) {
        getOptions().setWorkingDirectory(workingDirectory == null ? null : FileUtil.toSystemIndependentName(workingDirectory.toString()));
    }

    @Nullable
    @Override
    public String suggestedName() {
        String cmd = getCommand();
        String firstWord = cmd.contains(" ") ? cmd.substring(0, cmd.indexOf(' ')) : cmd;
        return StringUtil.capitalize(firstWord);
    }

    public static boolean getEmulateTerminalDefault() {
        return OpenApiUtil.isFeatureEnabled(RsExperiments.EMULATE_TERMINAL) &&
            !OpenApiUtil.isUnitTestMode();
    }
}
