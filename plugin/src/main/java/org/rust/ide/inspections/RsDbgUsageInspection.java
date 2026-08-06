/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections;

import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.project.workspace.PackageOrigin;
import org.rust.ide.fixes.RsQuickFixBase;
import org.rust.ide.utils.PsiModificationUtil;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.psi.ext.RsMacroCallUtil;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.PsiElementUtil;

public class RsDbgUsageInspection extends RsLocalInspectionTool {

    @Nonnull
@Override
    public RsVisitor buildVisitor(@Nonnull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitMacroExpr(@Nonnull RsMacroExpr o) {
                RsMacroCall macroCall = o.getMacroCall();
                if (!RsMacroCallUtil.getMacroName(macroCall).equals("dbg")) return;
                PsiElement resolvedMacro = macroCall.getPath().getReference() != null
                    ? macroCall.getPath().getReference().resolve() : null;
                if (resolvedMacro != null && ((RsElement) resolvedMacro).getContainingCrate().getOrigin() != PackageOrigin.STDLIB) return;
                if (!PsiModificationUtil.canReplace(macroCall)) return;
                holder.registerProblem(macroCall, RsBundle.message("dbg.usage"), new RsRemoveDbgQuickFix(macroCall, isOnTheFly));
            }
        };
    }

    private static class RsRemoveDbgQuickFix extends RsQuickFixBase<RsMacroCall> {
        private final boolean isOnTheFly;

        RsRemoveDbgQuickFix(@Nonnull RsMacroCall macroCall, boolean isOnTheFly) {
            super(macroCall);
            this.isOnTheFly = isOnTheFly;
        }

        @Nonnull
        @Override
        public consulo.localize.LocalizeValue getFamilyName() {
            return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.remove.dbg"));
            }

        @Nonnull
        @Override
        public consulo.localize.LocalizeValue getText() {
            return getFamilyName();
            }

        @Override
        public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull RsMacroCall element) {
            RsExprMacroArgument exprMacroArg = element.getExprMacroArgument();
            if (exprMacroArg == null) return;
            RsExpr expr = exprMacroArg.getExpr();
            if (expr == null) return;
            int cursorOffsetToExpr = editor != null ? Math.max(0, editor.getCaretModel().getOffset() - PsiElementUtil.getStartOffset(expr)) : -1;
            PsiElement parent = element.getParent().getParent();
            RsExpr newExpr;
            if (expr instanceof RsBinaryExpr && (parent instanceof RsBinaryExpr || parent instanceof RsDotExpr)) {
                if (editor != null) {
                    cursorOffsetToExpr += 1;
                }
                newExpr = (RsExpr) RsMacroCallUtil.replaceWithExpr(element, new RsPsiFactory(project).createExpression("(" + expr.getText() + ")"));
            } else {
                newExpr = (RsExpr) RsMacroCallUtil.replaceWithExpr(element, expr);
            }
            if (editor != null && isOnTheFly) {
                PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.getDocument());
                org.rust.openapiext.Editor.moveCaretToOffset(editor, newExpr, Math.min(PsiElementUtil.getStartOffset(newExpr) + cursorOffsetToExpr, PsiElementUtil.getEndOffset(newExpr)));
            }
        }
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("dbg.usage"));
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getGroupDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("rust"));
    }
}
