/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections;
import consulo.language.editor.inspection.SuppressQuickFix;

import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.LocalQuickFixOnPsiElement;
import consulo.language.editor.inspection.LocalQuickFixAndIntentionActionOnPsiElement;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.inspection.scheme.InspectionManager;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.LocalInspectionToolSession;
import consulo.component.PropertiesComponent;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.project.model.impl.CargoSettingsFilesService;
import org.rust.ide.fixes.AttachFileToModuleFix;
import org.rust.ide.fixes.ReloadProjectQuickFix;
import org.rust.lang.core.psi.RsFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import consulo.annotation.component.ExtensionImpl;

@ExtensionImpl
public class RsDetachedFileInspection extends RsLocalInspectionTool {

    private static final String NOTIFICATION_STATUS_KEY = "org.rust.disableDetachedFileInspection";

    @Nullable
    @Override
    public ProblemDescriptor[] checkFile(@Nonnull PsiFile file, @Nonnull InspectionManager manager, boolean isOnTheFly) {
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

    private boolean isInspectionEnabled(@Nonnull Project project, @Nonnull VirtualFile file) {
        return !project.getInstance(consulo.component.PropertiesComponent.class).getBoolean(getDisablingKey(file), false);
    }

    @Nonnull
    private static String getDisablingKey(@Nonnull VirtualFile file) {
        return NOTIFICATION_STATUS_KEY + file.getPath();
    }

    private static class SuppressFix implements SuppressQuickFix {
        @Nonnull
        @Override
        public consulo.localize.LocalizeValue getName() {
            return consulo.localize.LocalizeValue.of(RsBundle.message("intention.family.name.do.not.show.again"));
        }

        @Override
        public void applyFix(@Nonnull Project project, @Nonnull ProblemDescriptor descriptor) {
            PsiElement startElement = descriptor.getStartElement();
            if (!(startElement instanceof RsFile)) return;
            RsFile file = (RsFile) startElement;
            project.getInstance(consulo.component.PropertiesComponent.class).setValue(getDisablingKey(file.getVirtualFile()), true);
        }

        @Override
        public boolean isAvailable(@Nonnull Project project, @Nonnull PsiElement context) {
            return true;
        }

        public boolean isSuppressAll() {
            return false;
        }
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("inspection.rs.detached.file.display.name"));
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getGroupDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("rust"));
    }
}
