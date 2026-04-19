/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import org.jetbrains.annotations.NotNull;
import org.rust.RsBundle;
import org.rust.ide.fixes.SubstituteTextFix;
import org.rust.lang.core.psi.RsElseBranch;
import org.rust.lang.core.psi.RsIfExpr;
import org.rust.lang.core.psi.RsVisitor;
import org.rust.lang.core.psi.ext.PsiElementUtil;

public class RsDanglingElseInspection extends RsLocalInspectionTool {

    @Override
    public String getDisplayName() {
        return RsBundle.message("dangling.else");
    }

    @Override
    public RsVisitor buildVisitor(@NotNull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitElseBranch(@NotNull RsElseBranch expr) {
                PsiElement elseEl = expr.getElse();
                PsiElement breakEl = findBreakElement(elseEl);
                if (breakEl == null) return;
                PsiElement ifEl = findIfElement(breakEl);
                if (ifEl == null) return;
                holder.registerProblem(
                    expr,
                    new TextRange(0, ifEl.getStartOffsetInParent() + 2),
                    RsBundle.message("inspection.message.suspicious.else.if.formatting"),
                    SubstituteTextFix.delete(
                        RsBundle.message("intention.name.remove.else"),
                        expr.getContainingFile(),
                        PsiElementUtil.getRangeWithPrevSpace(elseEl, expr.getPrevSibling())
                    ),
                    SubstituteTextFix.replace(
                        RsBundle.message("intention.name.join.else.if"),
                        expr.getContainingFile(),
                        new TextRange(PsiElementUtil.getEndOffset(elseEl), PsiElementUtil.getStartOffset(ifEl)),
                        " "
                    )
                );
            }

            private PsiElement findBreakElement(PsiElement elseEl) {
                PsiElement current = elseEl.getNextSibling();
                while (current != null) {
                    if ((current instanceof PsiWhiteSpace || current instanceof PsiComment) && !current.getText().contains("\n")) {
                        current = current.getNextSibling();
                    } else {
                        return current;
                    }
                }
                return null;
            }

            private PsiElement findIfElement(PsiElement breakEl) {
                PsiElement current = breakEl.getNextSibling();
                while (current != null) {
                    if (current instanceof RsIfExpr) return current;
                    current = current.getNextSibling();
                }
                return null;
            }
        };
    }

    @Override
    public boolean isSyntaxOnly() {
        return true;
    }
}
