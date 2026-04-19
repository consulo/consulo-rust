/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.actions;

import com.intellij.injected.editor.EditorWindow;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import org.rust.ide.injected.RsDoctestLanguageInjector;
import org.rust.lang.core.psi.RsFile;

/**
 * This class is used to handle enter typing inside doctest language injection.
 */
public class RsEnterHandler extends EditorActionHandler {
    private final EditorActionHandler originalHandler;

    public RsEnterHandler(EditorActionHandler originalHandler) {
        this.originalHandler = originalHandler;
    }

    @Override
    protected boolean isEnabledForCaret(Editor editor, Caret caret, DataContext dataContext) {
        boolean isDoctestInjection = editor instanceof EditorWindow editorWindow &&
            editorWindow.getInjectedFile() instanceof RsFile rsFile &&
            RsDoctestLanguageInjector.isFileDoctestInjection(rsFile);
        return !isDoctestInjection && originalHandler.isEnabled(editor, caret, dataContext);
    }

    @Override
    protected void doExecute(Editor editor, Caret caret, DataContext dataContext) {
        originalHandler.execute(editor, caret, dataContext);
    }
}
