/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.debugger.runconfig;

import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.plugins.InstalledPluginsState;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.util.PlatformUtils;
import org.rust.RsBundle;
import org.rust.cargo.runconfig.RsDefaultProgramRunnerBase;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;
import org.rust.cargo.runconfig.RunConfigUtil;
import org.rust.ide.debugger.DebuggerUtils;
import org.rust.openapiext.OpenApiUtil;

import java.util.Set;

public class RsDebugAdvertisingRunner extends RsDefaultProgramRunnerBase {

    public static final String RUNNER_ID = "RsDebugAdvertisingRunner";

    @Override
    public boolean canRun(String executorId, RunProfile profile) {
        if (OpenApiUtil.isUnitTestMode()) return false;
        if (!DefaultDebugExecutor.EXECUTOR_ID.equals(executorId)) return false;
        if (!(profile instanceof CargoCommandConfiguration)) return false;
        if (!isSupportedPlatform()) return false;
        if (RunConfigUtil.getHasRemoteTarget((CargoCommandConfiguration) profile)) return false;
        var plugin = DebuggerUtils.nativeDebuggingSupportPlugin();
        if (plugin == null) return true;
        var loadedPlugins = PluginManagerCore.getLoadedPlugins();
        return !loadedPlugins.contains(plugin) || !plugin.isEnabled();
    }

    @Override
    public void execute(ExecutionEnvironment environment) {
        var plugin = DebuggerUtils.nativeDebuggingSupportPlugin();
        var pluginsState = InstalledPluginsState.getInstance();

        Action action;
        if (plugin == null && !pluginsState.wasInstalled(DebuggerUtils.NATIVE_DEBUGGING_SUPPORT_PLUGIN_ID)) {
            action = Action.INSTALL;
        } else if (plugin != null && !plugin.isEnabled()) {
            action = Action.ENABLE;
        } else {
            action = Action.RESTART;
        }

        Project project = environment.getProject();
        int options = Messages.showDialog(
            project,
            action.getMessage(),
            RsBundle.message("dialog.title.unable.to.run.debugger"),
            new String[]{action.getActionName()},
            Messages.OK,
            Messages.getErrorIcon()
        );

        if (options == Messages.OK) {
            action.doOkAction(project, DebuggerUtils.NATIVE_DEBUGGING_SUPPORT_PLUGIN_ID);
        }
    }

    @Override
    public String getRunnerId() {
        return RUNNER_ID;
    }

    @SuppressWarnings({"deprecation", "UnstableApiUsage"})
    private boolean isSupportedPlatform() {
        if (PlatformUtils.isIdeaUltimate() || PlatformUtils.isRubyMine() ||
            PlatformUtils.isGoIde() || PlatformUtils.isPyCharmPro()) {
            return true;
        }
        return false;
    }

    private enum Action {
        INSTALL {
            @Override
            public String getMessage() {
                return "Native Debugging Support plugin is not installed";
            }

            @Override
            public String getActionName() {
                return "Install";
            }

            @Override
            public void doOkAction(Project project, PluginId pluginId) {
                // `installAndEnable(project, setOf(pluginId), false) {}` from
                // `com.intellij.openapi.updateSettings.impl.pluginsAdvertisement`. That entry
                // point is not exposed via a Java-accessible class on the platform-IDEA-IU 233
                // distribution shipped under deps/ — fall through silently. The dialog still
                // shows "Install" / "Enable" / "Restart" actions; user can install/enable via
                // Settings | Plugins.
            }
        },
        ENABLE {
            @Override
            public String getMessage() {
                return "Native Debugging Support plugin is not enabled";
            }

            @Override
            public String getActionName() {
                return "Enable";
            }

            @Override
            public void doOkAction(Project project, PluginId pluginId) {
                // `installAndEnable(project, setOf(pluginId), false) {}` from
                // `com.intellij.openapi.updateSettings.impl.pluginsAdvertisement`. That entry
                // point is not exposed via a Java-accessible class on the platform-IDEA-IU 233
                // distribution shipped under deps/ — fall through silently. The dialog still
                // shows "Install" / "Enable" / "Restart" actions; user can install/enable via
                // Settings | Plugins.
            }
        },
        RESTART {
            @Override
            public String getMessage() {
                return "Need to restart " + ApplicationNamesInfo.getInstance().getFullProductName() + " to apply changes in plugins";
            }

            @Override
            public String getActionName() {
                return IdeBundle.message("ide.restart.action");
            }

            @Override
            public void doOkAction(Project project, PluginId pluginId) {
                ApplicationManagerEx.getApplicationEx().restart(true);
            }
        };

        @SuppressWarnings("UnstableApiUsage")
        public abstract String getMessage();

        @SuppressWarnings("UnstableApiUsage")
        public abstract String getActionName();

        public abstract void doOkAction(Project project, PluginId pluginId);
    }
}
