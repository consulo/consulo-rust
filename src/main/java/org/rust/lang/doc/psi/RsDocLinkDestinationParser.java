/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.doc.psi;

import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.impl.source.tree.TreeElement;
import com.intellij.util.CharTable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Parses doc link destinations for Rust intra-doc links.
 */
public final class RsDocLinkDestinationParser {

    private static final Set<String> KNOWN_PREFIXES = new HashSet<>(Arrays.asList(
        "struct", "enum", "trait", "union", "module", "mod", "const", "constant", "static",
        "function", "fn", "method", "derive", "type", "value", "macro", "prim", "primitive"
    ));

    private static final String[] KNOWN_SUFFIXES = {"!()", "!{}", "![]", "()", "!"};

    private RsDocLinkDestinationParser() {
    }

    @NotNull
    public static TreeElement parse(@NotNull CharSequence text, @NotNull CharTable charTable) {
        return docDataLeaf(text, charTable);
    }

    @Nullable
    public static TreeElement parseShortLink(@NotNull CharSequence text, @NotNull CharTable charTable) {
        // Simplified: return a DOC_DATA leaf
        if (text.length() == 0) return null;
        return docDataLeaf(text, charTable);
    }

    @NotNull
    private static LeafPsiElement docDataLeaf(@NotNull CharSequence text, @NotNull CharTable charTable) {
        return new LeafPsiElement(RsDocElementTypes.DOC_DATA, charTable.intern(text));
    }
}
