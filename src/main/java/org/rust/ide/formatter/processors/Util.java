/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.formatter.processors;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

/**
 * <p>
 * Contains utility methods for Rust formatting processors.
 */
public final class Util {

    private Util() {
    }

    /**
     * Determines whether the punctuation format processor should run for the given element.
     * Returns {@code false} if the element is invalid or punctuation preservation is enabled.
     */
    public static boolean shouldRunPunctuationProcessor(@NotNull ASTNode element) {
        return RsFormatProcessorUtil.shouldRunPunctuationProcessor(element);
    }
}
