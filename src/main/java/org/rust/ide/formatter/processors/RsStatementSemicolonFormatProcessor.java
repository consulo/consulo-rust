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
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.PsiElementUtil;

import java.util.ArrayList;
import java.util.List;

public class RsStatementSemicolonFormatProcessor implements PreFormatProcessor {

    @NotNull
    @Override
    public TextRange process(@NotNull ASTNode node, @NotNull TextRange range) {
        if (!RsFormatProcessorUtil.shouldRunPunctuationProcessor(node)) return range;

        List<PsiElement> elements = new ArrayList<>();

        node.getPsi().accept(new PsiRecursiveElementVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                if (range.contains(element.getTextRange())) {
                    super.visitElement(element);
                }

                // no semicolons inside "match"
                if (!(element.getParent() instanceof RsMatchArm)) {
                    if (element instanceof RsRetExpr || element instanceof RsBreakExpr || element instanceof RsContExpr) {
                        elements.add(element);
                    }
                }
            }
        });

        int addedCount = 0;
        for (PsiElement element : elements) {
            if (tryAddSemicolonAfter(element)) {
                addedCount++;
            }
        }
        return range.grown(addedCount);
    }

    private static boolean tryAddSemicolonAfter(@NotNull PsiElement element) {
        PsiElement nextSibling = PsiElementUtil.getNextNonCommentSibling(element);
        if (nextSibling == null || nextSibling.getNode().getElementType() != RsElementTypes.SEMICOLON) {
            RsPsiFactory psiFactory = new RsPsiFactory(element.getProject());
            element.getParent().addAfter(psiFactory.createSemicolon(), element);
            return true;
        }
        return false;
    }
}
