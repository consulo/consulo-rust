/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.typing.assist;

import consulo.language.editor.action.SmartEnterProcessorWithFixers;
import consulo.ui.ex.action.IdeActions;
import consulo.codeEditor.Editor;
import consulo.codeEditor.action.EditorActionManager;
import consulo.codeEditor.EditorEx;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsStructItem;
import consulo.codeEditor.action.EditorActionHandler;

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

        consulo.codeEditor.action.EditorActionHandler enterHandler =
            EditorActionManager.getInstance().getActionHandler(IdeActions.ACTION_EDITOR_ENTER);
        enterHandler.execute(editor, editor.getCaretModel().getCurrentCaret(), ((EditorEx) editor).getDataContext());
    }
}
