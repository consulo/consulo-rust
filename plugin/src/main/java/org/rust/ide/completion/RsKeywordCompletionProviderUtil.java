/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.completion;

import consulo.language.editor.completion.lookup.InsertionContext;
import jakarta.annotation.Nonnull;

/**
 * Delegates to methods in {@link RsKeywordCompletionProvider}.
 */
public final class RsKeywordCompletionProviderUtil {
    private RsKeywordCompletionProviderUtil() {
    }

    public static void addSuffix(@Nonnull InsertionContext ctx, @Nonnull String suffix) {
        RsKeywordCompletionProvider.addSuffix(ctx, suffix);
    }
}
