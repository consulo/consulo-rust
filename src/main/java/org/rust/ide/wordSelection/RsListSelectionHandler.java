/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.wordSelection;

import com.intellij.codeInsight.editorActions.ExtendWordSelectionHandlerBase;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.*;

import java.util.Collections;
import java.util.List;

public class RsListSelectionHandler extends ExtendWordSelectionHandlerBase {
    @Override
    public boolean canSelect(@NotNull PsiElement e) {
        return e instanceof RsTypeArgumentList || e instanceof RsValueArgumentList
            || e instanceof RsTypeParameterList || e instanceof RsValueParameterList;
    }

    @Override
    @Nullable
    public List<TextRange> select(@NotNull PsiElement e, @NotNull CharSequence editorText, int cursorOffset, @NotNull Editor editor) {
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
