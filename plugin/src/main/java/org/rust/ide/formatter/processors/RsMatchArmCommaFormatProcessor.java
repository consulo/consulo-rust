/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.formatter.processors;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.ast.ASTNode;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiRecursiveElementVisitor;
import consulo.language.codeStyle.PreFormatProcessor;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsBlockExpr;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsMatchArm;
import org.rust.lang.core.psi.ext.impl.PsiElementUtil;

@ExtensionImpl
public class RsMatchArmCommaFormatProcessor implements PreFormatProcessor {

    @Nonnull
    @Override
    public TextRange process(@Nonnull ASTNode element, @Nonnull TextRange range) {
        if (!RsFormatProcessorUtil.shouldRunPunctuationProcessor(element)) return range;

        final int[] nRemovedCommas = {0};
        element.getPsi().accept(new PsiRecursiveElementVisitor() {
            @Override
            public void visitElement(@Nonnull PsiElement element) {
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

    private static boolean removeCommaAfterBlock(@Nonnull RsMatchArm element) {
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
