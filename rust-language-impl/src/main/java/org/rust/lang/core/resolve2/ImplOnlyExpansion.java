/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import consulo.language.ast.IElementType;
import consulo.language.ast.TokenType;
import consulo.language.lexer.Lexer;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.lexer.RsLexer;
import org.rust.lang.core.psi.impl.RsTokenSets;

import static org.rust.lang.core.psi.RsElementTypes.*;

/**
 * Whether the text a macro call expands to holds nothing but impls. A def map takes the names a module declares, and an
 * impl declares none, so such an expansion need not be parsed - and most of the module-level macro calls of the
 * standard library are of this kind ({@code int_impl!}, {@code partial_eq_impl!}, ...), with large expansions.
 */
final class ImplOnlyExpansion {
    private ImplOnlyExpansion() {
    }

    /**
     * Whether every item of {@code text} is an impl, maybe with attributes, {@code unsafe} or {@code default}: anything
     * else - another item, a macro call - may declare a name, and makes the answer {@code false}.
     */
    static boolean isImplOnly(@Nonnull CharSequence text) {
        Lexer lexer = new RsLexer();
        lexer.start(text);
        boolean sawImpl = false;
        while (true) {
            IElementType type = next(lexer);
            if (type == null) return sawImpl;
            // the attributes of the item: `#[...]`
            while (type == SHA) {
                type = next(lexer);
                if (type == EXCL) type = next(lexer);
                if (type != LBRACK || !skipGroup(lexer, LBRACK, RBRACK)) return false;
                type = next(lexer);
            }
            while (type == UNSAFE || type == DEFAULT) type = next(lexer);
            if (type != IMPL) return false;
            // the header up to the body; a `{` in the header can only be a const generic argument
            int angle = 0;
            while (true) {
                type = next(lexer);
                if (type == null) return false;
                if (type == LT) angle++;
                else if (type == GT) angle--;
                else if (type == GTGT) angle -= 2;
                else if (type == LBRACE && angle <= 0) break;
                else if (type == LBRACE || type == LPAREN || type == LBRACK) {
                    IElementType close = type == LBRACE ? RBRACE : type == LPAREN ? RPAREN : RBRACK;
                    if (!skipGroup(lexer, type, close)) return false;
                }
            }
            if (!skipGroup(lexer, LBRACE, RBRACE)) return false;
            sawImpl = true;
        }
    }

    /** Skips to the token that closes the group whose opening token was just read; {@code false} at the end of the text. */
    private static boolean skipGroup(@Nonnull Lexer lexer, @Nonnull IElementType open, @Nonnull IElementType close) {
        int depth = 1;
        while (depth > 0) {
            IElementType type = next(lexer);
            if (type == null) return false;
            if (type == open) depth++;
            else if (type == close) depth--;
        }
        return true;
    }

    /** The next token that is not whitespace or a comment, or {@code null} at the end. */
    private static IElementType next(@Nonnull Lexer lexer) {
        while (true) {
            IElementType type = lexer.getTokenType();
            if (type == null) return null;
            lexer.advance();
            if (type != TokenType.WHITE_SPACE && !RsTokenSets.RS_COMMENTS.contains(type)) return type;
        }
    }
}
