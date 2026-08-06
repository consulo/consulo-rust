/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions.addFmtStringArgument;

import consulo.document.Document;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.util.EditorUtil;
import consulo.project.Project;
import consulo.language.editor.ui.awt.EditorTextField;
import jakarta.annotation.Nonnull;
import org.rust.lang.RsFileType;

import java.awt.event.KeyListener;

public class RsAddFmtStringArgumentEditorTextField extends EditorTextField {
    public RsAddFmtStringArgumentEditorTextField(@Nonnull Project project, @Nonnull Document document) {
        super(document, project, RsFileType.INSTANCE, false, true);
    }

    @Override
    @Nonnull
    protected EditorEx createEditor() {
        EditorEx editor = super.createEditor();
        editor.setHorizontalScrollbarVisible(false);
        editor.setVerticalScrollbarVisible(false);
        editor.getSettings().setUseSoftWraps(false);
        editor.getSettings().setLineCursorWidth(2); // EditorUtil.getDefaultCaretWidth isn't exposed in Consulo
        editor.getColorsScheme().setEditorFontName(getFont().getFontName());
        editor.getColorsScheme().setEditorFontSize(getFont().getSize());
        return editor;
    }

    // onEditorAdded isn't exposed on Consulo's EditorTextField; key listener wiring skipped

    @Override
    public synchronized void removeKeyListener(KeyListener l) {
        super.removeKeyListener(l);
        Editor ed = getEditor();
        if (ed != null) {
            ed.getContentComponent().removeKeyListener(l);
        }
    }
}
