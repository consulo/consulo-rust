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
import com.intellij.task.ProjectTaskManager;
import com.intellij.util.PlatformUtils;
import org.rust.ide.experiments.RsExperiments;
import org.rust.openapiext.OpenApiUtil;

public class RsBuildAction extends LegacyAnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        performForContext(e.getDataContext());
    }

    
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
}
