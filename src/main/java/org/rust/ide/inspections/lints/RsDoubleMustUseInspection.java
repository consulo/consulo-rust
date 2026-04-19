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

/** Analogue of Clippy's double_must_use. */
public class RsDoubleMustUseInspection extends RsLintInspection {

    @NotNull
    @Override
    protected RsLint getLint(@NotNull PsiElement element) {
        return RsLint.DoubleMustUse;
    }

    @NotNull
    @Override
    public RsVisitor buildVisitor(@NotNull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitFunction2(@NotNull RsFunction o) {
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

        FixRemoveMustUseAttr(@NotNull PsiElement element) {
            super(element);
        }

        @NotNull
        @Override
        public String getText() {
            return RsBundle.message("inspection.DoubleMustUse.FixRemoveMustUseAttr.name");
        }

        @NotNull
        @Override
        public String getFamilyName() {
            return getText();
        }

        @Override
        public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull PsiElement element) {
            element.delete();
        }
    }
}
