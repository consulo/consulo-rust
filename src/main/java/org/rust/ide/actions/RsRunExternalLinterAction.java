/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.actions;

import com.intellij.analysis.AnalysisScope;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.actions.RunInspectionIntention;
import com.intellij.codeInspection.ex.InspectionManagerEx;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.profile.codeInspection.InspectionProjectProfileManager;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.runconfig.command.RunCargoCommandActionBase;
import org.rust.cargo.runconfig.RunConfigUtil;
import org.rust.ide.inspections.RsExternalLinterInspection;

public class RsRunExternalLinterAction extends RunCargoCommandActionBase {

    public static final Key<CargoProject> CARGO_PROJECT = Key.create("Cargo project");

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        var currentProfile = InspectionProjectProfileManager.getInstance(project).getCurrentProfile();
        var wrapper = currentProfile.getInspectionTool(RsExternalLinterInspection.SHORT_NAME, project);
        if (wrapper == null) return;
        InspectionManagerEx managerEx = (InspectionManagerEx) InspectionManager.getInstance(project);
        var inspectionContext = RunInspectionIntention.createContext(wrapper, managerEx, null);

        CargoProject cargoProject = RunConfigUtil.getAppropriateCargoProject(e.getDataContext());
        inspectionContext.putUserData(CARGO_PROJECT, cargoProject);

        inspectionContext.doInspections(new AnalysisScope(project));
    }
}
