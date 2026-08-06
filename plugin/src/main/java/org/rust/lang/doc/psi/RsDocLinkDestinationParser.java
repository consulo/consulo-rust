/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.doc.psi;

import consulo.language.impl.psi.LeafPsiElement;
import consulo.language.impl.ast.TreeElement;
import consulo.language.util.CharTable;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

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

    @Nonnull
    public static TreeElement parse(@Nonnull CharSequence text, @Nonnull CharTable charTable) {
        return docDataLeaf(text, charTable);
    }

    @Nullable
    public static TreeElement parseShortLink(@Nonnull CharSequence text, @Nonnull CharTable charTable) {
        // Simplified: return a DOC_DATA leaf
        if (text.length() == 0) return null;
        return docDataLeaf(text, charTable);
    }

    @Nonnull
    private static LeafPsiElement docDataLeaf(@Nonnull CharSequence text, @Nonnull CharTable charTable) {
        return new LeafPsiElement(RsDocElementTypes.DOC_DATA, charTable.intern(text));
    }
}
