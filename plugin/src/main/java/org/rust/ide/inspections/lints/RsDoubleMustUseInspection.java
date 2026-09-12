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
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsMetaItem;
import org.rust.lang.core.psi.RsVisitor;
import org.rust.lang.core.psi.ext.RsAttr;
import org.rust.lang.core.psi.ext.RsAttrOwnerExtUtil;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.ty.TyAdt;
import org.rust.lang.core.types.ty.Ty;

import java.util.Collections;
import java.util.List;
import consulo.localize.LocalizeValue;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.annotation.component.ExtensionImpl;
import org.rust.lang.core.psi.ext.RsFunctionUtil;

/** Analogue of Clippy's double_must_use. */
@ExtensionImpl
public class RsDoubleMustUseInspection extends RsLintInspection {

    @Nonnull
    @Override
    protected RsLint getLint(@Nonnull PsiElement element) {
        return RsLint.DoubleMustUse;
    }

    @Nonnull
    @Override
    public RsVisitor buildVisitor(@Nonnull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitFunction2(@Nonnull RsFunction o) {
                String mustUseAttrName = "must_use";
                RsMetaItem metaItemOnFunc = RsAttrOwnerExtUtil.findFirstMetaItem(o, mustUseAttrName);
                Ty returnType = org.rust.lang.core.psi.ext.RsFunctionUtil.getNormReturnType(o);
                TyAdt type = returnType instanceof TyAdt ? (TyAdt) returnType : null;
                RsMetaItem attrType = type != null ? RsAttrOwnerExtUtil.findFirstMetaItem(type.getItem(), mustUseAttrName) : null;
                if (metaItemOnFunc != null && attrType != null) {
                    String description = RsBundle.message("inspection.DoubleMustUse.description");
                    RsLintHighlightingType highlighting = RsLintHighlightingType.WEAK_WARNING;
                    PsiElement parent = metaItemOnFunc.getParent();
                    PsiElement attr = parent instanceof RsAttr ? parent : metaItemOnFunc;
                    List<FixRemoveMustUseAttr> fixes = attr instanceof RsAttr
                        ? Collections.singletonList(new FixRemoveMustUseAttr(attr))
                        : Collections.emptyList();
                    registerLintProblem(holder, attr, description, highlighting, Collections.unmodifiableList(fixes));
                }
            }
        };
    }

    private static class FixRemoveMustUseAttr extends RsQuickFixBase<PsiElement> {

        FixRemoveMustUseAttr(@Nonnull PsiElement element) {
            super(element);
        }

        @Nonnull
        @Override
        public consulo.localize.LocalizeValue getText() {
            return consulo.localize.LocalizeValue.of(RsBundle.message("inspection.DoubleMustUse.FixRemoveMustUseAttr.name"));
            }

        @Nonnull
        @Override
        public consulo.localize.LocalizeValue getFamilyName() {
            return getText();
            }

        @Override
        public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull PsiElement element) {
            element.delete();
        }
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("inspection.rs.double.must.use.display.name"));
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getGroupDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("lints"));
    }

    @Nonnull
    @Override
    public LocalizeValue[] getGroupPath() {
        return new LocalizeValue[]{LocalizeValue.of(RsBundle.message("rust"))};
    }

    @Nonnull
    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.WEAK_WARNING;
    }
}
