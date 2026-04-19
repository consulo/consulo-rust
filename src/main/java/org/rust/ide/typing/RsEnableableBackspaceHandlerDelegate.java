/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.typing;

import com.intellij.codeInsight.editorActions.BackspaceHandlerDelegate;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;

public abstract class RsEnableableBackspaceHandlerDelegate extends BackspaceHandlerDelegate {
    private boolean myEnabled = false;

    @Override
    public void beforeCharDeleted(char c, PsiFile file, Editor editor) {
        myEnabled = deleting(c, file, editor);
    }

    @Override
    public boolean charDeleted(char c, PsiFile file, Editor editor) {
        if (!myEnabled) return false;
        return deleted(c, file, editor);
    }

    /**
     * Determine whether this handler applies to given context and perform necessary actions before deleting c.
     */
    public boolean deleting(char c, PsiFile file, Editor editor) {
        return true;
    }

    /**
     * Perform action after char c was deleted.
     *
     * @return true whether this handler succeeded and the IDE should stop evaluating
     *         remaining handlers; otherwise, false
     */
    public boolean deleted(char c, PsiFile file, Editor editor) {
        return false;
    }
}
