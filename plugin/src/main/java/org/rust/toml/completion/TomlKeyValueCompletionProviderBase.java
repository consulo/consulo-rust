/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.completion;

import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionProvider;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.psi.PsiElement;
import consulo.language.util.ProcessingContext;
import jakarta.annotation.Nonnull;
import org.rust.toml.Util;
import org.toml.lang.psi.TomlKeySegment;
import org.toml.lang.psi.TomlKeyValue;

public abstract class TomlKeyValueCompletionProviderBase implements CompletionProvider {
    @Override
    public void addCompletions(@Nonnull CompletionParameters parameters,
                                  @Nonnull ProcessingContext context,
                                  @Nonnull CompletionResultSet result) {
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

    protected abstract void completeKey(@Nonnull TomlKeyValue keyValue, @Nonnull CompletionResultSet result);

    protected abstract void completeValue(@Nonnull TomlKeyValue keyValue, @Nonnull CompletionResultSet result);
}
