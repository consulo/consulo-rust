/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

    @NotNull
    @Override
    public String getDisplayName() {
        return RsBundle.message("dbg.usage");
    }

    @Override
    public RsVisitor buildVisitor(@NotNull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitMacroExpr(@NotNull RsMacroExpr o) {
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

        RsRemoveDbgQuickFix(@NotNull RsMacroCall macroCall, boolean isOnTheFly) {
            super(macroCall);
            this.isOnTheFly = isOnTheFly;
        }

        @NotNull
        @Override
        public String getFamilyName() {
            return RsBundle.message("intention.name.remove.dbg");
        }

        @NotNull
        @Override
        public String getText() {
            return getFamilyName();
        }

        @Override
        public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull RsMacroCall element) {
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
}
