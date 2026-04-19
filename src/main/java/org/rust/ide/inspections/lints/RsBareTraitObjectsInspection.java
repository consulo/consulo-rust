/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.ide.fixes.RsQuickFixBase;
import org.rust.ide.inspections.RsProblemsHolder;
import org.rust.ide.inspections.RsWithMacrosInspectionVisitor;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsTypeReferenceExtUtil;
import org.rust.lang.core.psi.ext.RsTraitTypeExtUtil;
import org.rust.lang.core.psi.ext.RsEditionsUtil;
import org.rust.lang.core.resolve.ref.RsReference;
import org.rust.lang.utils.RsDiagnostic;
// import removed
import org.rust.lang.core.types.RsTypesUtil;

public class RsBareTraitObjectsInspection extends RsLintInspection {

    @NotNull
    @Override
    protected RsLint getLint(@NotNull PsiElement element) {
        return RsLint.BareTraitObjects;
    }

    @NotNull
    @Override
    public RsVisitor buildVisitor(@NotNull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitTypeReference(@NotNull RsTypeReference typeReference) {
                if (!org.rust.lang.core.psi.ext.RsElementUtil.isAtLeastEdition2018(typeReference)) return;

                PsiElement skipped = RsTypeReferenceExtUtil.skipParens(typeReference);
                RsTraitType traitType = skipped instanceof RsTraitType ? (RsTraitType) skipped : null;
                RsPath typePath = skipped instanceof RsPathType ? ((RsPathType) skipped).getPath() : null;
                boolean isTraitType = traitType != null
                    || (typePath != null && typePath.getReference() != null
                        && org.rust.lang.core.resolve.ref.RsPathReferenceImpl.deepResolve((org.rust.lang.core.resolve.ref.RsPathReference) typePath.getReference()) instanceof RsTraitItem);
                boolean isSelf = typePath != null && typePath.getCself() != null;
                boolean hasDyn = traitType != null && RsTraitTypeExtUtil.getDyn(traitType) != null;
                boolean hasImpl = traitType != null && traitType.getImpl() != null;
                if (!isTraitType || isSelf || hasDyn || hasImpl) return;

                RsDiagnostic.addToHolder(
                    new RsDiagnostic.TraitObjectWithNoDyn(typeReference, new AddDynKeywordFix(typeReference)),
                    holder
                );
            }
        };
    }

    private static class AddDynKeywordFix extends RsQuickFixBase<RsTypeReference> {

        AddDynKeywordFix(@NotNull RsTypeReference element) {
            super(element);
        }

        @NotNull
        @Override
        public String getText() {
            return RsBundle.message("intention.name.add.dyn.keyword.to.trait.object");
        }

        @NotNull
        @Override
        public String getFamilyName() {
            return getText();
        }

        @Override
        public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull RsTypeReference element) {
            PsiElement typeElement = RsTypeReferenceExtUtil.skipParens(element);
            String traitText;
            if (typeElement instanceof RsPathType) {
                traitText = ((RsPathType) typeElement).getPath().getText();
            } else {
                traitText = ((RsTraitType) typeElement).getText();
            }
            RsTypeReference newRef = new RsPsiFactory(project).createDynTraitType(traitText);
            element.replace(newRef);
        }
    }
}
