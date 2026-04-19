/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.formatter.processors;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiRecursiveElementVisitor;
import com.intellij.psi.impl.source.codeStyle.PreFormatProcessor;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsBlockExpr;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsMatchArm;
import org.rust.lang.core.psi.ext.PsiElementUtil;

public class RsMatchArmCommaFormatProcessor implements PreFormatProcessor {

    @NotNull
    @Override
    public TextRange process(@NotNull ASTNode element, @NotNull TextRange range) {
        if (!RsFormatProcessorUtil.shouldRunPunctuationProcessor(element)) return range;

        final int[] nRemovedCommas = {0};
        element.getPsi().accept(new PsiRecursiveElementVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                if (range.contains(element.getTextRange())) {
                    super.visitElement(element);
                }

                if (element instanceof RsMatchArm && removeCommaAfterBlock((RsMatchArm) element)) {
                    nRemovedCommas[0] += 1;
                }
            }
        });
        return range.grown(-nRemovedCommas[0]);
    }

    private static boolean removeCommaAfterBlock(@NotNull RsMatchArm element) {
        PsiElement expr = element.getExpr();
        if (expr == null) return false;
        if (!(expr instanceof RsBlockExpr) || ((RsBlockExpr) expr).getUnsafe() != null) return false;
        PsiElement comma = PsiElementUtil.getNextNonCommentSibling(expr);
        if (comma == null) return false;
        if (comma.getNode().getElementType() == RsElementTypes.COMMA) {
            comma.delete();
            return true;
        }
        return false;
    }
}
