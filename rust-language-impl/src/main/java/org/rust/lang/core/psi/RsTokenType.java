/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi;

import consulo.language.ast.IElementType;
import consulo.language.ast.TokenSet;
import jakarta.annotation.Nonnull;
import org.rust.lang.RsLanguage;
import org.rust.lang.doc.psi.RsDocCommentElementType;

/**
 * The element type of a Rust lexer token. Instances of this class are created while the generated
 * element type table is being built, so this class must not hold any state derived from that table;
 * the token sets live in {@link RsTokenSets} instead.
 */
public class RsTokenType extends IElementType {

    public RsTokenType(@Nonnull String debugName) {
        super(debugName, RsLanguage.INSTANCE);
    }

    @Nonnull
    public static TokenSet tokenSetOf(@Nonnull IElementType... tokens) {
        return TokenSet.create(tokens);
    }

    @Nonnull
    public static final RsTokenType BLOCK_COMMENT = new RsTokenType("<BLOCK_COMMENT>");
    @Nonnull
    public static final RsTokenType EOL_COMMENT = new RsTokenType("<EOL_COMMENT>");
    @Nonnull
    public static final RsDocCommentElementType INNER_BLOCK_DOC_COMMENT = new RsDocCommentElementType("<INNER_BLOCK_DOC_COMMENT>");
    @Nonnull
    public static final RsDocCommentElementType OUTER_BLOCK_DOC_COMMENT = new RsDocCommentElementType("<OUTER_BLOCK_DOC_COMMENT>");
    @Nonnull
    public static final RsDocCommentElementType INNER_EOL_DOC_COMMENT = new RsDocCommentElementType("<INNER_EOL_DOC_COMMENT>");
    @Nonnull
    public static final RsDocCommentElementType OUTER_EOL_DOC_COMMENT = new RsDocCommentElementType("<OUTER_EOL_DOC_COMMENT>");
}
