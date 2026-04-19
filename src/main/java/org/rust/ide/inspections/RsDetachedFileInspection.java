/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections;

import com.intellij.codeInspection.*;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.project.model.impl.CargoSettingsFilesService;
import org.rust.ide.fixes.AttachFileToModuleFix;
import org.rust.ide.fixes.ReloadProjectQuickFix;
import org.rust.lang.core.psi.RsFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class RsDetachedFileInspection extends RsLocalInspectionTool {

    private static final String NOTIFICATION_STATUS_KEY = "org.rust.disableDetachedFileInspection";

    @Nullable
    @Override
    public ProblemDescriptor[] checkFile(@NotNull PsiFile file, @NotNull InspectionManager manager, boolean isOnTheFly) {
        if (!(file instanceof RsFile)) return null;
        RsFile rsFile = (RsFile) file;
        Project project = file.getProject();

        if (!isInspectionEnabled(project, rsFile.getVirtualFile())) return null;

        var cargoProjects = CargoProjectServiceUtil.getCargoProjects(project);
        if (!cargoProjects.getInitialized()) return null;

        var cargoProject = cargoProjects.findProjectForFile(rsFile.getVirtualFile());
        if (cargoProject == null) return null;
        if (cargoProject.getWorkspace() == null) return null;

        if (rsFile.getCrateRoot() == null) {
            VirtualFile virtualFile = rsFile.getVirtualFile();
            var pkg = CargoProjectServiceUtil.getCargoProjects(project).findPackageForFile(virtualFile);
            if (pkg == null) return null;
            Set<VirtualFile> implicitTargets = CargoSettingsFilesService.collectImplicitTargets(pkg);
            LocalQuickFix mainFix;
            if (implicitTargets.contains(virtualFile)) {
                mainFix = new ReloadProjectQuickFix();
            } else {
                mainFix = AttachFileToModuleFix.createIfCompatible(project, rsFile);
            }

            List<LocalQuickFix> fixes = new ArrayList<>();
            if (mainFix != null) fixes.add(mainFix);
            fixes.add(new SuppressFix());

            return new ProblemDescriptor[]{
                manager.createProblemDescriptor(file,
                    RsBundle.message("inspection.message.file.not.included.in.module.tree.analysis.not.available"),
                    isOnTheFly,
                    fixes.toArray(new LocalQuickFix[0]),
                    ProblemHighlightType.WARNING
                )
            };
        }

        return null;
    }

    @Override
    public boolean isSyntaxOnly() {
        return true;
    }

    private boolean isInspectionEnabled(@NotNull Project project, @NotNull VirtualFile file) {
        return !PropertiesComponent.getInstance(project).getBoolean(getDisablingKey(file), false);
    }

    @NotNull
    private static String getDisablingKey(@NotNull VirtualFile file) {
        return NOTIFICATION_STATUS_KEY + file.getPath();
    }

    private static class SuppressFix implements SuppressQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return RsBundle.message("intention.family.name.do.not.show.again");
        }

        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            PsiElement startElement = descriptor.getStartElement();
            if (!(startElement instanceof RsFile)) return;
            RsFile file = (RsFile) startElement;
            PropertiesComponent.getInstance(project).setValue(getDisablingKey(file.getVirtualFile()), true);
        }

        @Override
        public boolean isAvailable(@NotNull Project project, @NotNull PsiElement context) {
            return true;
        }

        @Override
        public boolean isSuppressAll() {
            return false;
        }
    }
}
