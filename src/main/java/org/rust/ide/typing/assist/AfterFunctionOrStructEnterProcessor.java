/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.typing.assist;

import com.intellij.lang.SmartEnterProcessorWithFixers;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionManager;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsStructItem;

public class AfterFunctionOrStructEnterProcessor extends SmartEnterProcessorWithFixers.FixEnterProcessor {

    @Override
    public boolean doEnter(PsiElement atCaret, PsiFile file, Editor editor, boolean modified) {
        if (!modified) return false;

        PsiElement parent = atCaret.getParent();
        if (parent instanceof RsFunction || parent instanceof RsStructItem) {
            int elementEndOffset = parent.getTextRange().getEndOffset() - 2;
            editor.getCaretModel().moveToOffset(elementEndOffset);
            plainEnter(editor);
            return modified;
        }
        return false;
    }

    @Override
    protected void plainEnter(Editor editor) {
        if (!(editor instanceof EditorEx)) return;

        com.intellij.openapi.editor.actionSystem.EditorActionHandler enterHandler =
            EditorActionManager.getInstance().getActionHandler(IdeActions.ACTION_EDITOR_ENTER);
        enterHandler.execute(editor, editor.getCaretModel().getCurrentCaret(), ((EditorEx) editor).getDataContext());
    }
}
