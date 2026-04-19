/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.wordSelection;

import com.intellij.codeInsight.editorActions.ExtendWordSelectionHandlerBase;
import com.intellij.codeInsight.editorActions.SelectWordUtil;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.lexer.RsEscapesLexer;
import org.rust.lang.core.psi.RsLiteralKind;

import org.rust.lang.core.psi.RsTokenType;
import org.rust.lang.core.psi.ext.PsiElementUtil;

import java.util.ArrayList;
import java.util.List;

public class RsStringLiteralSelectionHandler extends ExtendWordSelectionHandlerBase {
    @Override
    public boolean canSelect(@NotNull PsiElement e) {
        return RsTokenType.RS_ALL_STRING_LITERALS.contains(PsiElementUtil.getElementType(e));
    }

    @Override
    @Nullable
    public List<TextRange> select(@NotNull PsiElement e, @NotNull CharSequence editorText, int cursorOffset, @NotNull Editor editor) {
        RsLiteralKind kind = RsLiteralKind.fromAstNode(e.getNode());
        if (!(kind instanceof RsLiteralKind.StringLiteral)) return null;
        RsLiteralKind.StringLiteral stringKind = (RsLiteralKind.StringLiteral) kind;
        TextRange valueRange = stringKind.getOffsets().getValue();
        if (valueRange == null) return null;
        valueRange = valueRange.shiftRight(kind.getNode().getStartOffset());

        List<TextRange> result = super.select(e, editorText, cursorOffset, editor);
        if (result == null) {
            result = new ArrayList<>();
        }

        IElementType elementType = PsiElementUtil.getElementType(e);
        if (RsEscapesLexer.ESCAPABLE_LITERALS_TOKEN_SET.contains(elementType)) {
            SelectWordUtil.addWordHonoringEscapeSequences(
                editorText,
                valueRange,
                cursorOffset,
                RsEscapesLexer.of(elementType),
                result
            );
        }

        result.add(valueRange);
        return result;
    }
}
