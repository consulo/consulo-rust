/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
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
