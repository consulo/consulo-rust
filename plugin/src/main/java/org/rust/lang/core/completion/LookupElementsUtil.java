/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import java.util.List;

/**
 * Bridge class delegating to {@link LookupElements}.
 */
public final class LookupElementsUtil {
    private LookupElementsUtil() {
    }

    public static LookupElement withPriority(LookupElementBuilder builder, double priority) {
        return LookupElements.withPriority(builder, priority);
    }
}
