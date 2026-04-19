/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.wordSelection;

import com.intellij.codeInsight.editorActions.ExtendWordSelectionHandlerBase;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.PsiElementUtil;
import org.rust.lang.core.psi.ext.RsElement;

import java.util.List;

public class RsBlockSelectionHandler extends ExtendWordSelectionHandlerBase {
    @Override
    public boolean canSelect(@NotNull PsiElement e) {
        return e instanceof RsBlock || e instanceof RsBlockFields || e instanceof RsStructLiteralBody
            || e instanceof RsEnumBody || e instanceof RsMembers || e instanceof RsMatchBody;
    }

    @Override
    public List<TextRange> select(@NotNull PsiElement e, @NotNull CharSequence editorText, int cursorOffset, @NotNull Editor editor) {
        PsiElement startNode = null;
        for (PsiElement child = e.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (PsiElementUtil.getElementType(child) == RsElementTypes.LBRACE) {
                // find first non-whitespace sibling after LBRACE
                for (PsiElement sibling = child.getNextSibling(); sibling != null; sibling = sibling.getNextSibling()) {
                    if (!(sibling instanceof PsiWhiteSpace)) {
                        startNode = sibling;
                        break;
                    }
                }
                break;
            }
        }
        if (startNode == null) return null;

        PsiElement endNode = null;
        for (PsiElement sibling = startNode.getNextSibling(); sibling != null; sibling = sibling.getNextSibling()) {
            if (PsiElementUtil.getElementType(sibling) == RsElementTypes.RBRACE) {
                // find last non-whitespace sibling before RBRACE
                for (PsiElement prev = sibling.getPrevSibling(); prev != null; prev = prev.getPrevSibling()) {
                    if (!(prev instanceof PsiWhiteSpace)) {
                        endNode = prev;
                        break;
                    }
                }
                break;
            }
        }
        if (endNode == null) return null;

        int startOffset = startNode.getTextRange().getStartOffset();
        int endOffset = endNode.getTextRange().getEndOffset();
        if (startOffset >= endOffset) return null;

        TextRange range = TextRange.create(startOffset, endOffset);
        return expandToWholeLine(editorText, range);
    }
}
