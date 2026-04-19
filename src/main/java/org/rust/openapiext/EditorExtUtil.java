/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.openapiext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;

/**
 * Bridge class delegating to {@link EditorExt}.
 */
public final class EditorExtUtil {
    private EditorExtUtil() {
    }

    public static void setSelection(@NotNull Editor editor, @NotNull PsiElement context, int startOffset, int endOffset) {
        EditorExt.setSelection(editor, context, startOffset, endOffset);
    }

    public static void moveCaretToOffset(@NotNull Editor editor, @NotNull PsiElement context, int absoluteOffsetInFile) {
        EditorExt.moveCaretToOffset(editor, context, absoluteOffsetInFile);
    }
}
