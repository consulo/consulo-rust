/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig;

import consulo.execution.configuration.ConfigurationFactory;
import consulo.execution.configuration.LocatableConfigurationBase;
import com.intellij.execution.configurations.RunConfigurationWithSuppressedDefaultDebugAction;
import consulo.execution.configuration.RunProfileState;
import consulo.project.Project;
import org.jdom.Element;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.project.model.CargoProjectsService;
import org.rust.ide.experiments.RsExperiments;
import org.rust.openapiext.OpenApiUtil;
import consulo.util.lang.StringUtil;

import java.nio.file.Path;
import java.util.Collection;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;

public abstract class RsCommandConfiguration extends LocatableConfigurationBase
    implements RunConfigurationWithSuppressedDefaultDebugAction,
    consulo.execution.configuration.RunProfileWithCompileBeforeLaunchOption {

    /**
     * The Rust modules of the project. The platform builds the compile scope from these before
     * launching, which is what routes the build through the toolchain on the module's Rust extension.
     */
    @Nonnull
    @Override
    public consulo.module.Module[] getModules() {
        java.util.List<consulo.module.Module> modules = new java.util.ArrayList<>();
        for (consulo.module.Module module : consulo.module.ModuleManager.getInstance(getProject()).getModules()) {
            if (consulo.rust.module.extension.RustModuleExtension.findExtension(module) != null) {
                modules.add(module);
            }
        }
        return modules.toArray(consulo.module.Module.EMPTY_ARRAY);
    }

    /**
     * Without a Rust module there is nothing cargo could build, so an empty module list must not
     * fall back to compiling the whole project.
     */
    @Override
    public boolean isBuildProjectOnEmptyModuleList() {
        return false;
    }

    private String myCommand;
    private boolean myEmulateTerminal;
    @Nullable
    private Path myWorkingDirectory;

    protected RsCommandConfiguration(@Nonnull Project project, @Nonnull String name, @Nonnull ConfigurationFactory factory) {
        super(project, factory, name);
        myEmulateTerminal = getEmulateTerminalDefault();
        if (!project.isDefault()) {
            CargoProjectsService cargoProjects = CargoProjectServiceUtil.getCargoProjects(project);
            Collection<CargoProject> allProjects = cargoProjects.getAllProjects();
            if (!allProjects.isEmpty()) {
                myWorkingDirectory = org.rust.cargo.runconfig.command.CargoCommandConfiguration.getWorkingDirectory(allProjects.iterator().next());
            }
        }
    }

    @Nonnull
    public abstract String getCommand();

    public abstract void setCommand(@Nonnull String command);

    public boolean getEmulateTerminal() {
        return myEmulateTerminal;
    }

    public void setEmulateTerminal(boolean emulateTerminal) {
        myEmulateTerminal = emulateTerminal;
    }

    @Nullable
    public Path getWorkingDirectory() {
        return myWorkingDirectory;
    }

    public void setWorkingDirectory(@Nullable Path workingDirectory) {
        myWorkingDirectory = workingDirectory;
    }

    @Nullable
    @Override
    public String suggestedName() {
        String cmd = getCommand();
        String firstWord = cmd.contains(" ") ? cmd.substring(0, cmd.indexOf(' ')) : cmd;
        return StringUtil.capitalize(firstWord);
    }

    @Override
    public void writeExternal(@Nonnull Element element) {
        super.writeExternal(element);
        RunConfigUtil.writeString(element, "command", getCommand());
        RunConfigUtil.writePath(element, "workingDirectory", myWorkingDirectory);
        RunConfigUtil.writeBool(element, "emulateTerminal", myEmulateTerminal);
    }

    @Override
    public void readExternal(@Nonnull Element element) {
        super.readExternal(element);
        String command = RunConfigUtil.readString(element, "command");
        if (command != null) setCommand(command);
        Path wd = RunConfigUtil.readPath(element, "workingDirectory");
        if (wd != null) myWorkingDirectory = wd;
        Boolean et = RunConfigUtil.readBool(element, "emulateTerminal");
        if (et != null) myEmulateTerminal = et;
    }

    public static boolean getEmulateTerminalDefault() {
        return org.rust.openapiext.OpenApiUtil.isFeatureEnabled(RsExperiments.EMULATE_TERMINAL) &&
            !org.rust.openapiext.OpenApiUtil.isUnitTestMode();
    }
}
