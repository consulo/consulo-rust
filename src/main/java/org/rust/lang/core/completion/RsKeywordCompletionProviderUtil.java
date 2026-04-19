/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion;

import com.intellij.codeInsight.completion.InsertionContext;
import org.jetbrains.annotations.NotNull;

/**
 * Delegates to methods in {@link RsKeywordCompletionProvider}.
 */
public final class RsKeywordCompletionProviderUtil {
    private RsKeywordCompletionProviderUtil() {
    }

    public static void addSuffix(@NotNull InsertionContext ctx, @NotNull String suffix) {
        RsKeywordCompletionProvider.addSuffix(ctx, suffix);
    }
}
