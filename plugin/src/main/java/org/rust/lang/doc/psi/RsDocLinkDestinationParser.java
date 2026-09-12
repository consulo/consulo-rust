/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.doc.psi;

import consulo.language.impl.ast.TreeElement;
import consulo.language.impl.psi.LeafPsiElement;
import consulo.language.util.CharTable;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Builds the AST of a doc link destination.
 * <p>
 * The destination is currently kept as plain text: it is not re-parsed as a Rust path, so intra-doc
 * links are not resolved. Whatever this class returns must cover the whole {@code text} it is given,
 * otherwise the doc comment AST stops matching the comment token.
 */
public final class RsDocLinkDestinationParser {

    private RsDocLinkDestinationParser() {
    }

    /**
     * Parses the destination of an inline or a reference link, e.g. {@code bar} in {@code [foo](bar)}.
     */
    @Nonnull
    public static TreeElement parse(@Nonnull CharSequence text, @Nonnull CharTable charTable) {
        return new LeafPsiElement(RsDocElementTypes.DOC_DATA, charTable.intern(text));
    }

    /**
     * Parses a short reference link, e.g. {@code [foo]}, as a Rust path.
     *
     * @return {@code null} when the link is not a path, in which case it keeps its Markdown structure
     */
    @Nullable
    public static TreeElement parseShortLink(@Nonnull CharSequence text, @Nonnull CharTable charTable) {
        return null;
    }
}
