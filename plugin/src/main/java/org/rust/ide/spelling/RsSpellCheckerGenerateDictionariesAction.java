/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.spelling;

import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.language.editor.LangDataKeys;
import consulo.module.Module;
import consulo.project.Project;
import consulo.module.content.ModuleRootManager;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ui.ex.action.LegacyAnAction;
import jakarta.annotation.Nonnull;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.project.model.CargoProjectsService;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.cargo.project.workspace.PackageOrigin;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRefAnchor;
import consulo.annotation.component.ActionRef;

@ActionImpl(
    id = "Rust.GenerateDictionaries",
    parents = @ActionParentRef(
        value = @ActionRef(id = "Internal")
    )
)
public class RsSpellCheckerGenerateDictionariesAction extends LegacyAnAction {

    private static final List<String> EXCLUDE_DIRS = Arrays.asList("tests", "benches");

    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getData(consulo.project.Project.KEY);
        if (project == null) return;
        Module module = e.getData(LangDataKeys.MODULE);
        if (module == null) return;
        VirtualFile[] contentRoots = ModuleRootManager.getInstance(module).getContentRoots();
        if (contentRoots.length == 0) return;
        String contextRootPath = contentRoots[0].getPath();

        RsSpellCheckerDictionaryGenerator generator =
            new RsSpellCheckerDictionaryGenerator(project, contextRootPath + "/dicts");

        CargoProjectsService cargoProjects = CargoProjectServiceUtil.getCargoProjects(project);
        Collection<CargoProject> allProjects = cargoProjects.getAllProjects();
        CargoProject cargoProject = allProjects.isEmpty() ? null : allProjects.iterator().next();
        if (cargoProject == null) return;

        CargoWorkspace workspace = cargoProject.getWorkspace();
        if (workspace == null) return;

        for (CargoWorkspace.Package pkg : workspace.getPackages()) {
            if (pkg.getOrigin() != PackageOrigin.STDLIB) continue;
            VirtualFile contentRoot = pkg.getContentRoot();
            if (contentRoot == null) continue;
            generator.addFolder("rust", contentRoot);
            // do not analyze "non-production" code since it contains identifiers like "aaaa", "aaba", etc.
            for (String dir : EXCLUDE_DIRS) {
                VirtualFile child = contentRoot.findChild(dir);
                if (child != null) {
                    generator.excludeFolder(child);
                }
            }
        }
        generator.generate();
    }


    @Override
    public void update(@Nonnull AnActionEvent e) {
        Project project = e.getData(consulo.project.Project.KEY);
        boolean visible = project != null && CargoProjectServiceUtil.getCargoProjects(project).getHasAtLeastOneValidProject();
        e.getPresentation().setEnabledAndVisible(visible);
    }
}
