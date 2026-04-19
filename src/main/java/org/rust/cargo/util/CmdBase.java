/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.util;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import org.rust.lang.core.completion.RsKeywordCompletionProvider;

import java.util.List;

public abstract class CmdBase {
    private final String name;
    private final LookupElement lookupElement;

    public CmdBase(String name) {
        this.name = name;
        this.lookupElement = LookupElementBuilder.create(name).withInsertHandler((ctx, item) -> {
            RsKeywordCompletionProvider.addSuffix(ctx, " ");
        });
    }

    public String getName() {
        return name;
    }

    public abstract List<Opt> getOptions();

    public LookupElement getLookupElement() {
        return lookupElement;
    }
}
