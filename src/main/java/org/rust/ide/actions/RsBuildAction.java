/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.actions;

import com.google.common.annotations.VisibleForTesting;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.intellij.task.ProjectTaskManager;
import com.intellij.util.PlatformUtils;
import org.rust.ide.experiments.RsExperiments;
import org.rust.openapiext.OpenApiUtil;

public class RsBuildAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        performForContext(e.getDataContext());
    }

    @VisibleForTesting
    public void performForContext(DataContext e) {
        Project project = OpenApiUtil.getProject(e);
        if (project == null) return;
        if (OpenApiUtil.isFeatureEnabled(RsExperiments.BUILD_TOOL_WINDOW)) {
            ProjectTaskManager.getInstance(project).buildAllModules();
        } else {
            org.rust.cargo.runconfig.RunConfigUtil.buildProject(project);
        }
    }

    @Override
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(AnActionEvent e) {
        super.update(e);
        Project project = e.getProject();
        e.getPresentation().setEnabledAndVisible(
            isSuitablePlatform() && project != null && org.rust.cargo.runconfig.RunConfigUtil.hasCargoProject(project)
        );
    }

    private static boolean isSuitablePlatform() {
        return !(PlatformUtils.isIntelliJ() || PlatformUtils.isAppCode() || PlatformUtils.isCLion());
    }
}
