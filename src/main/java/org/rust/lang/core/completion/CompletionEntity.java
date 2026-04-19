/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion;

import com.intellij.codeInsight.lookup.LookupElementBuilder;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.resolve.KnownItems;
import org.rust.lang.core.types.ty.Ty;

public interface CompletionEntity {
    @Nullable
    Ty retTy(KnownItems items);

    RsLookupElementProperties getBaseLookupElementProperties(RsCompletionContext context);

    LookupElementBuilder createBaseLookupElement(RsCompletionContext context);
}
