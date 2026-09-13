/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.completion;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import org.rust.lang.core.completion.LookupElements;

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
