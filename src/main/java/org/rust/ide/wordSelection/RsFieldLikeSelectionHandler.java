/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.wordSelection;

import com.intellij.codeInsight.editorActions.ExtendWordSelectionHandlerBase;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.PsiElementUtil;

import java.util.List;

public class RsFieldLikeSelectionHandler extends ExtendWordSelectionHandlerBase {
    @Override
    public boolean canSelect(@NotNull PsiElement e) {
        return isFieldLikeDecl(e);
    }

    @Override
    public List<TextRange> select(@NotNull PsiElement e, @NotNull CharSequence editorText, int cursorOffset, @NotNull Editor editor) {
        int start = e.getTextRange().getStartOffset();
        int end = e.getTextRange().getEndOffset();

        // expand the end to include the adjacent comma after the field:
        for (PsiElement sibling = e.getNextSibling(); sibling != null; sibling = sibling.getNextSibling()) {
            if (!(sibling instanceof PsiComment) && !(sibling instanceof PsiWhiteSpace)) {
                if (PsiElementUtil.getElementType(sibling) == RsElementTypes.COMMA) {
                    end = sibling.getTextRange().getEndOffset();
                }
                break;
            }
        }

        return expandToWholeLine(editorText, TextRange.create(start, end));
    }

    public static boolean isFieldLikeDecl(@NotNull PsiElement e) {
        return e instanceof RsNamedFieldDecl || e instanceof RsStructLiteralField
            || e instanceof RsEnumVariant || e instanceof RsMatchArm;
    }
}
