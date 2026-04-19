/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.resolve;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.project.model.CargoProjectsService;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.lang.core.psi.RsFile;
import org.rust.openapiext.VirtualFileExtUtil;
import org.toml.lang.psi.TomlKeySegment;

public class CargoDependencyReferenceProvider extends PsiReferenceProvider {
    @NotNull
    @Override
    public PsiReference[] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
        if (!(element instanceof TomlKeySegment)) return PsiReference.EMPTY_ARRAY;
        return new PsiReference[]{new CargoDependencyReferenceImpl((TomlKeySegment) element)};
    }

    private static class CargoDependencyReferenceImpl extends PsiReferenceBase<TomlKeySegment> {
        public CargoDependencyReferenceImpl(@NotNull TomlKeySegment key) {
            super(key);
        }

        @Nullable
        @Override
        public PsiElement resolve() {
            TomlKeySegment elem = getElement();
            CargoProjectsService cargoProjects = CargoProjectServiceUtil.getCargoProjects(elem.getProject());
            if (elem.getContainingFile() == null || elem.getContainingFile().getVirtualFile() == null) return null;
            CargoProject cargoProject = cargoProjects.findProjectForFile(elem.getContainingFile().getVirtualFile());
            if (cargoProject == null) return null;
            CargoWorkspace workspace = cargoProject.getWorkspace();
            if (workspace == null) return null;
            CargoWorkspace.Package pkg = workspace.findPackageByName(elem.getText());
            if (pkg == null) return null;
            CargoWorkspace.Target libTarget = pkg.getLibTarget();
            if (libTarget == null) return null;
            if (libTarget.getCrateRoot() == null) return null;
            PsiElement psiFile = VirtualFileExtUtil.toPsiFile(libTarget.getCrateRoot(), elem.getProject());
            if (psiFile instanceof RsFile) return psiFile;
            return null;
        }

        @NotNull
        @Override
        public Object[] getVariants() {
            return LookupElement.EMPTY_ARRAY;
        }

        @NotNull
        @Override
        protected TextRange calculateDefaultRangeInElement() {
            return TextRange.from(0, getElement().getTextLength());
        }
    }
}
