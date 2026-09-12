/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.wordSelection;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.action.ExtendWordSelectionHandlerBase;
import consulo.codeEditor.Editor;
import consulo.document.util.TextRange;
import consulo.application.util.LineTokenizer;
import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiWhiteSpace;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsMembers;
import org.rust.lang.core.psi.RsStmt;
import org.rust.lang.core.psi.ext.PsiElementUtil;

import java.util.ArrayList;
import java.util.List;

@ExtensionImpl
public class RsGroupSelectionHandler extends ExtendWordSelectionHandlerBase {
    @Override
    public boolean canSelect(@Nonnull PsiElement e) {
        return e instanceof RsStmt || RsFieldLikeSelectionHandler.isFieldLikeDecl(e)
            || e.getParent() instanceof RsMembers || e instanceof PsiComment;
    }

    @Override
    public List<TextRange> select(@Nonnull PsiElement e, @Nonnull CharSequence editorText, int cursorOffset, @Nonnull Editor editor) {
        // see com.intellij.codeInsight.editorActions.wordSelection.StatementGroupSelectioner for the reference implementation

        List<TextRange> result = new ArrayList<>();

        PsiElement startElement = e;
        PsiElement endElement = e;

        while (startElement.getPrevSibling() != null) {
            PsiElement sibling = startElement.getPrevSibling();

            if (PsiElementUtil.getElementType(sibling) == RsElementTypes.LBRACE) break;

            if (sibling instanceof PsiWhiteSpace) {
                String[] strings = LineTokenizer.tokenize(sibling.getText().toCharArray(), false);
                if (strings.length > 2) {
                    break;
                }
            }
            startElement = sibling;
        }

        while (startElement instanceof PsiWhiteSpace) startElement = startElement.getNextSibling();

        while (endElement.getNextSibling() != null) {
            PsiElement sibling = endElement.getNextSibling();

            if (PsiElementUtil.getElementType(sibling) == RsElementTypes.RBRACE) break;

            if (sibling instanceof PsiWhiteSpace) {
                String[] strings = LineTokenizer.tokenize(sibling.getText().toCharArray(), false);
                if (strings.length > 2) {
                    break;
                }
            }
            endElement = sibling;
        }

        while (endElement instanceof PsiWhiteSpace) endElement = endElement.getPrevSibling();

        result.addAll(
            expandToWholeLine(
                editorText, new TextRange(startElement.getTextRange().getStartOffset(), endElement.getTextRange().getEndOffset())
            )
        );

        return result;
    }
}
