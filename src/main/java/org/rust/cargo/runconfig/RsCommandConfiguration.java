/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.LocatableConfigurationBase;
import com.intellij.execution.configurations.RunConfigurationWithSuppressedDefaultDebugAction;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.openapi.project.Project;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.project.model.CargoProjectsService;
import org.rust.ide.experiments.RsExperiments;
import org.rust.openapiext.OpenApiUtil;
import com.intellij.openapi.util.text.StringUtil;

import java.nio.file.Path;
import java.util.Collection;

public abstract class RsCommandConfiguration extends LocatableConfigurationBase<RunProfileState>
    implements RunConfigurationWithSuppressedDefaultDebugAction {

    private String myCommand;
    private boolean myEmulateTerminal;
    @Nullable
    private Path myWorkingDirectory;

    protected RsCommandConfiguration(@NotNull Project project, @NotNull String name, @NotNull ConfigurationFactory factory) {
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

    @NotNull
    public abstract String getCommand();

    public abstract void setCommand(@NotNull String command);

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
    public void writeExternal(@NotNull Element element) {
        super.writeExternal(element);
        RunConfigUtil.writeString(element, "command", getCommand());
        RunConfigUtil.writePath(element, "workingDirectory", myWorkingDirectory);
        RunConfigUtil.writeBool(element, "emulateTerminal", myEmulateTerminal);
    }

    @Override
    public void readExternal(@NotNull Element element) {
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
