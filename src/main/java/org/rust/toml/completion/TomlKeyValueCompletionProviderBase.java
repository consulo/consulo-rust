/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.rust.toml.Util;
import org.toml.lang.psi.TomlKeySegment;
import org.toml.lang.psi.TomlKeyValue;

public abstract class TomlKeyValueCompletionProviderBase extends CompletionProvider<CompletionParameters> {
    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters,
                                  @NotNull ProcessingContext context,
                                  @NotNull CompletionResultSet result) {
        PsiElement parent = parameters.getPosition().getParent();
        if (parent == null) return;
        if (parent instanceof TomlKeySegment) {
            PsiElement keyParent = parent.getParent();
            if (keyParent == null) throw new IllegalStateException("PsiElementPattern must not allow keys outside of TomlKeyValues");
            PsiElement kvCandidate = keyParent.getParent();
            if (!(kvCandidate instanceof TomlKeyValue)) {
                throw new IllegalStateException("PsiElementPattern must not allow keys outside of TomlKeyValues");
            }
            completeKey((TomlKeyValue) kvCandidate, result);
        } else {
            TomlKeyValue keyValue = Util.getClosestKeyValueAncestor(parameters.getPosition());
            if (keyValue == null) return;
            completeValue(keyValue, result);
        }
    }

    protected abstract void completeKey(@NotNull TomlKeyValue keyValue, @NotNull CompletionResultSet result);

    protected abstract void completeValue(@NotNull TomlKeyValue keyValue, @NotNull CompletionResultSet result);
}
