/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.actions;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.action.JoinLinesHandlerDelegate;
import consulo.document.Document;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.util.lang.CharSequenceSubSequence;
import org.rust.ide.formatter.impl.RsFmtImplUtil;
import org.rust.ide.typing.TypingUtil;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsFile;
import org.rust.lang.core.psi.ext.PsiElementUtil;
import org.rust.lang.core.psi.RsTokenSets;
import consulo.language.ast.IElementType;

@ExtensionImpl(id = "RsJoinLinesHandler")
public class RsJoinLinesHandler implements JoinLinesHandlerDelegate {

    /**
     * Fixup lines after they have been joined.
     */
    @Override
    public int tryJoinLines(Document document, PsiFile file, int offsetNear, int end) {
        if (!(file instanceof RsFile)) return CANNOT_JOIN;

        PsiElement leftPsi = file.findElementAt(offsetNear);
        if (leftPsi == null) return CANNOT_JOIN;
        PsiElement rightPsi = file.findElementAt(end);
        if (rightPsi == null) return CANNOT_JOIN;

        int tryJoinCommaList = joinCommaList(document, leftPsi, rightPsi);
        if (tryJoinCommaList != CANNOT_JOIN) return tryJoinCommaList;

        if (leftPsi != rightPsi) return CANNOT_JOIN;

        if (RsTokenSets.RS_STRING_LITERALS.contains(PsiElementUtil.getElementType(leftPsi))) {
            return joinStringLiteral(document, offsetNear, end);
        }

        return CANNOT_JOIN;
    }

    private int joinCommaList(Document document, PsiElement leftPsi, PsiElement rightPsi) {
        if (PsiElementUtil.getElementType(leftPsi) != RsElementTypes.COMMA) return CANNOT_JOIN;
        if (leftPsi.getParent() == null) return CANNOT_JOIN;
        consulo.language.ast.IElementType parentType = PsiElementUtil.getElementType(leftPsi.getParent());
        consulo.language.ast.IElementType rightType = PsiElementUtil.getElementType(rightPsi);
        RsFmtImplUtil.CommaList list = RsFmtImplUtil.CommaList.forElement(parentType);
        if (list == null) return CANNOT_JOIN;
        if (rightType == list.getClosingBrace()) {
            String replaceWith = list.getNeedsSpaceBeforeClosingBrace() ? " " : "";
            document.replaceString(leftPsi.getTextOffset(), rightPsi.getTextOffset(), replaceWith);
            return leftPsi.getTextOffset();
        }
        return CANNOT_JOIN;
    }

    private int joinStringLiteral(Document document, int offsetNear, int end) {
        CharSequence text = document.getCharsSequence();

        int start = offsetNear;

        // Strip newline escape
        if (TypingUtil.endsWithUnescapedBackslash(new CharSequenceSubSequence(text, 0, start + 1))) {
            start--;
            while (start >= 0 && (text.charAt(start) == ' ' || text.charAt(start) == '\t')) {
                start--;
            }
        }

        document.deleteString(start + 1, end);
        document.insertString(start + 1, " ");

        return start + 1;
    }
}
