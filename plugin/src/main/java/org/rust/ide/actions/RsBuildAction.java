/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.actions;

import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.dataContext.DataContext;
import consulo.project.Project;
import consulo.ui.ex.action.LegacyAnAction;
import com.intellij.task.ModuleBuildTask;
import com.intellij.task.ProjectTaskContext;
import consulo.module.Module;
import consulo.module.ModuleManager;
import org.rust.cargo.runconfig.buildtool.CargoBuildTaskRunner;
import com.intellij.util.PlatformUtils;
import org.rust.ide.experiments.RsExperiments;
import org.rust.openapiext.OpenApiUtil;
import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRefAnchor;
import consulo.annotation.component.ActionRef;
import org.rust.cargo.runconfig.RunConfigUtil;

@ActionImpl(
    id = "Rust.Build",
    parents = {
    @ActionParentRef(
        value = @ActionRef(id = "RunnerActions"),
        anchor = ActionRefAnchor.BEFORE,
        relatedToAction = @ActionRef(id = "RunConfiguration")
    ),
    @ActionParentRef(
        value = @ActionRef(id = "TouchBarDefault"),
        anchor = ActionRefAnchor.BEFORE,
        relatedToAction = @ActionRef(id = "RunConfiguration")
    )
    },
    shortcutFrom = @ActionRef(id = "CompileDirty")
)
public class RsBuildAction extends LegacyAnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        performForContext(e.getDataContext());
    }

    
    public void performForContext(DataContext e) {
        Project project = OpenApiUtil.getProject(e);
        if (project == null) return;
        if (OpenApiUtil.isFeatureEnabled(RsExperiments.BUILD_TOOL_WINDOW)) {
            CargoBuildTaskRunner runner = new CargoBuildTaskRunner();
            // The runner only accepts a module it recognises as Cargo-backed, and it expands an
            // accepted task into one build task per Cargo project of the whole project - so the
            // first accepted module already covers the entire workspace.
            for (Module module : ModuleManager.getInstance(project).getModules()) {
                ModuleBuildTask task = new AllModulesBuildTask(module);
                if (runner.canRun(task)) {
                    runner.run(project, new ProjectTaskContext(), task);
                    return;
                }
            }
        }
        // No module the build runner would accept: run cargo build directly.
        RunConfigUtil.buildProject(project);
    }


    @Override
    public void update(AnActionEvent e) {
        super.update(e);
        Project project = e.getData(consulo.project.Project.KEY);
        e.getPresentation().setEnabledAndVisible(
            isSuitablePlatform() && project != null && org.rust.cargo.runconfig.RunConfigUtil.hasCargoProject(project)
        );
    }

    private static boolean isSuitablePlatform() {
        return !(PlatformUtils.isIntelliJ() || PlatformUtils.isAppCode() || PlatformUtils.isCLion());
    }

    /** Incremental build request covering every Cargo project of the module's project. */
    private record AllModulesBuildTask(Module module) implements ModuleBuildTask {
        @Override
        public Module getModule() {
            return module;
        }

        @Override
        public boolean isIncrementalBuild() {
            return true;
        }

        @Override
        public boolean isIncludeDependentModules() {
            return true;
        }

        @Override
        public boolean isIncludeRuntimeDependencies() {
            return true;
        }

        @Override
        public boolean isIncludeTests() {
            return false;
        }

        @Override
        public String getPresentableName() {
            return "build";
        }
    }
}
