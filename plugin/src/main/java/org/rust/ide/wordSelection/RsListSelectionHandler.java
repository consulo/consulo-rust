/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.wordSelection;

import consulo.language.editor.action.ExtendWordSelectionHandlerBase;
import consulo.language.ast.ASTNode;
import consulo.codeEditor.Editor;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.*;

import java.util.Collections;
import java.util.List;

public class RsListSelectionHandler extends ExtendWordSelectionHandlerBase {
    @Override
    public boolean canSelect(@Nonnull PsiElement e) {
        return e instanceof RsTypeArgumentList || e instanceof RsValueArgumentList
            || e instanceof RsTypeParameterList || e instanceof RsValueParameterList;
    }

    @Override
    @Nullable
    public List<TextRange> select(@Nonnull PsiElement e, @Nonnull CharSequence editorText, int cursorOffset, @Nonnull Editor editor) {
        ASTNode node = e.getNode();
        if (node == null) return null;
        ASTNode startNode = node.findChildByType(RsTokenType.RS_LIST_OPEN_SYMBOLS);
        if (startNode == null) return null;
        ASTNode endNode = node.findChildByType(RsTokenType.RS_LIST_CLOSE_SYMBOLS);
        if (endNode == null) return null;
        TextRange range = new TextRange(startNode.getStartOffset() + 1, endNode.getStartOffset());
        return Collections.singletonList(range);
    }
}
