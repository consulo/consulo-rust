/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.wordSelection;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.action.ExtendWordSelectionHandlerBase;
import consulo.codeEditor.Editor;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiWhiteSpace;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.PsiElementUtil;

import java.util.List;

@ExtensionImpl
public class RsFieldLikeSelectionHandler extends ExtendWordSelectionHandlerBase {
    @Override
    public boolean canSelect(@Nonnull PsiElement e) {
        return isFieldLikeDecl(e);
    }

    @Override
    public List<TextRange> select(@Nonnull PsiElement e, @Nonnull CharSequence editorText, int cursorOffset, @Nonnull Editor editor) {
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

    public static boolean isFieldLikeDecl(@Nonnull PsiElement e) {
        return e instanceof RsNamedFieldDecl || e instanceof RsStructLiteralField
            || e instanceof RsEnumVariant || e instanceof RsMatchArm;
    }
}
