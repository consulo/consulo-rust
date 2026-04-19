/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions.addFmtStringArgument;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.project.Project;
import com.intellij.ui.EditorTextField;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.RsFileType;

import java.awt.event.KeyListener;

public class RsAddFmtStringArgumentEditorTextField extends EditorTextField {
    public RsAddFmtStringArgumentEditorTextField(@NotNull Project project, @NotNull Document document) {
        super(document, project, RsFileType.INSTANCE, false, true);
    }

    @Override
    @NotNull
    protected EditorEx createEditor() {
        EditorEx editor = super.createEditor();
        editor.setHorizontalScrollbarVisible(false);
        editor.setVerticalScrollbarVisible(false);
        editor.getSettings().setUseSoftWraps(false);
        editor.getSettings().setLineCursorWidth(EditorUtil.getDefaultCaretWidth());
        editor.getColorsScheme().setEditorFontName(getFont().getFontName());
        editor.getColorsScheme().setEditorFontSize(getFont().getSize());
        return editor;
    }

    @Override
    protected void onEditorAdded(@NotNull Editor editor) {
        super.onEditorAdded(editor);
        for (KeyListener listener : getKeyListeners()) {
            editor.getContentComponent().addKeyListener(listener);
        }
        editor.getContentComponent().setFocusTraversalKeysEnabled(false);
    }

    @Override
    public synchronized void removeKeyListener(KeyListener l) {
        super.removeKeyListener(l);
        Editor ed = getEditor();
        if (ed != null) {
            ed.getContentComponent().removeKeyListener(l);
        }
    }
}
