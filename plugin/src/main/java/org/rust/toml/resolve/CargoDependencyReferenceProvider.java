/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.resolve;

import consulo.language.editor.completion.lookup.LookupElement;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.PsiReferenceBase;
import consulo.language.psi.PsiReferenceProvider;
import consulo.language.util.ProcessingContext;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.project.model.CargoProjectsService;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.lang.core.psi.RsFile;
import org.rust.openapiext.VirtualFileExtUtil;
import org.toml.lang.psi.TomlKeySegment;

public class CargoDependencyReferenceProvider extends PsiReferenceProvider {
    @Nonnull
    @Override
    public PsiReference[] getReferencesByElement(@Nonnull PsiElement element, @Nonnull ProcessingContext context) {
        if (!(element instanceof TomlKeySegment)) return PsiReference.EMPTY_ARRAY;
        return new PsiReference[]{new CargoDependencyReferenceImpl((TomlKeySegment) element)};
    }

    private static class CargoDependencyReferenceImpl extends PsiReferenceBase<TomlKeySegment> {
        public CargoDependencyReferenceImpl(@Nonnull TomlKeySegment key) {
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

        @Nonnull
        @Override
        public Object[] getVariants() {
            return LookupElement.EMPTY_ARRAY;
        }

        @Nonnull
        @Override
        protected TextRange calculateDefaultRangeInElement() {
            return TextRange.from(0, getElement().getTextLength());
        }
    }
}
