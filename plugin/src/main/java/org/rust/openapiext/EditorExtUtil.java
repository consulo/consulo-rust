/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.openapiext;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;

/**
 * Bridge class delegating to {@link EditorExt}.
 */
public final class EditorExtUtil {
    private EditorExtUtil() {
    }

    public static void setSelection(@Nonnull Editor editor, @Nonnull PsiElement context, int startOffset, int endOffset) {
        EditorExt.setSelection(editor, context, startOffset, endOffset);
    }

    public static void moveCaretToOffset(@Nonnull Editor editor, @Nonnull PsiElement context, int absoluteOffsetInFile) {
        EditorExt.moveCaretToOffset(editor, context, absoluteOffsetInFile);
    }
}
