/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints;

import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.ide.fixes.RsQuickFixBase;
import org.rust.ide.inspections.RsProblemsHolder;
import org.rust.ide.inspections.RsWithMacrosInspectionVisitor;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsTypeReferenceExtUtil;
import org.rust.lang.core.psi.ext.RsTraitTypeExtUtil;
import org.rust.lang.core.resolve.ref.RsReference;
import org.rust.lang.utils.RsDiagnostic;
// import removed
import org.rust.lang.core.types.RsTypesUtil;
import consulo.localize.LocalizeValue;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.annotation.component.ExtensionImpl;
import org.rust.lang.core.psi.ext.RsElementUtil;
import org.rust.lang.core.resolve.ref.RsPathReference;
import org.rust.lang.core.resolve.ref.RsPathReferenceImpl;

@ExtensionImpl
public class RsBareTraitObjectsInspection extends RsLintInspection {

    @Nonnull
    @Override
    protected RsLint getLint(@Nonnull PsiElement element) {
        return RsLint.BareTraitObjects;
    }

    @Nonnull
    @Override
    public RsVisitor buildVisitor(@Nonnull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitTypeReference(@Nonnull RsTypeReference typeReference) {
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

        AddDynKeywordFix(@Nonnull RsTypeReference element) {
            super(element);
        }

        @Nonnull
        @Override
        public consulo.localize.LocalizeValue getText() {
            return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.add.dyn.keyword.to.trait.object"));
            }

        @Nonnull
        @Override
        public consulo.localize.LocalizeValue getFamilyName() {
            return getText();
            }

        @Override
        public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull RsTypeReference element) {
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

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("inspection.rs.bare.trait.objects.display.name"));
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getGroupDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("rust"));
    }

    @Nonnull
    @Override
    public LocalizeValue[] getGroupPath() {
        return new LocalizeValue[]{LocalizeValue.of(RsBundle.message("rust"))};
    }

    @Nonnull
    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.ERROR;
    }
}
