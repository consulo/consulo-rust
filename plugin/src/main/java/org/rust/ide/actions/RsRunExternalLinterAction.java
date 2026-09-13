/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.actions;

import consulo.language.editor.scope.AnalysisScope;
import consulo.language.editor.inspection.scheme.InspectionManager;
import consulo.ide.impl.idea.codeInspection.actions.RunInspectionIntention;
import consulo.ide.impl.idea.codeInspection.ex.InspectionManagerImpl;
import consulo.ui.ex.action.AnActionEvent;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.language.editor.inspection.scheme.InspectionProjectProfileManager;
import org.rust.cargo.api.model.CargoProject;
import org.rust.cargo.runconfig.command.RunCargoCommandActionBase;
import org.rust.cargo.runconfig.RunConfigUtil;
import org.rust.ide.inspections.RsExternalLinterInspection;
import consulo.annotation.component.ActionImpl;
import consulo.rust.localize.RustLocalize;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;

@ActionImpl(id = "Cargo.RunExternalLinter")
public class RsRunExternalLinterAction extends RunCargoCommandActionBase {

    public RsRunExternalLinterAction() {
        super(RustLocalize.actionCargoRunexternallinterText(), LocalizeValue.empty(), PlatformIconGroup.actionsLightning());
    }

    public static final Key<CargoProject> CARGO_PROJECT = Key.create("Cargo project");

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(consulo.project.Project.KEY);
        if (project == null) return;

        var currentProfile = InspectionProjectProfileManager.getInstance(project).getCurrentProfile();
        var wrapper = currentProfile.getInspectionTool(RsExternalLinterInspection.SHORT_NAME, project);
        if (wrapper == null) return;
        InspectionManagerImpl managerEx = (InspectionManagerImpl) InspectionManager.getInstance(project);
        var inspectionContext = RunInspectionIntention.createContext(wrapper, managerEx, null);

        CargoProject cargoProject = RunConfigUtil.getAppropriateCargoProject(e.getDataContext());
        inspectionContext.putUserData(CARGO_PROJECT, cargoProject);

        inspectionContext.doInspections(new AnalysisScope(project));
    }
}
