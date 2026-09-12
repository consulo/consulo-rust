/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.actions;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.codeEditor.action.ExtensionEditorActionHandler;
import consulo.dataContext.DataContext;
import consulo.language.editor.inject.EditorWindow;
import consulo.ui.ex.action.IdeActions;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.ide.injected.RsDoctestLanguageInjector;
import org.rust.lang.core.psi.RsFile;

/**
 * This class is used to handle enter typing inside doctest language injection.
 */
@ExtensionImpl
public class RsEnterHandler extends EditorActionHandler implements ExtensionEditorActionHandler {
    private EditorActionHandler myOriginalHandler;

    @Override
    public void init(@Nullable EditorActionHandler originalHandler) {
        myOriginalHandler = originalHandler;
    }

    @Nonnull
    @Override
    public String getActionId() {
        return IdeActions.ACTION_EDITOR_ENTER;
    }

    @Override
    protected boolean isEnabledForCaret(Editor editor, Caret caret, DataContext dataContext) {
        boolean isDoctestInjection = editor instanceof EditorWindow editorWindow &&
            editorWindow.getInjectedFile() instanceof RsFile rsFile &&
            RsDoctestLanguageInjector.isFileDoctestInjection(rsFile);
        return !isDoctestInjection && myOriginalHandler != null && myOriginalHandler.isEnabled(editor, caret, dataContext);
    }

    @Override
    protected void doExecute(Editor editor, Caret caret, DataContext dataContext) {
        if (myOriginalHandler != null) {
            myOriginalHandler.execute(editor, caret, dataContext);
        }
    }
}
