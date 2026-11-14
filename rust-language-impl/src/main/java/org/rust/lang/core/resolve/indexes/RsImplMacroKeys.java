/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve.indexes;

import consulo.language.ast.IElementType;
import consulo.language.ast.TokenType;
import consulo.language.lexer.Lexer;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.lexer.RsLexer;
import org.rust.lang.core.psi.impl.RsTokenSets;
import org.rust.lang.core.types.TyFingerprint;
import org.rust.lang.core.types.ty.TyFloat;
import org.rust.lang.core.types.ty.TyInteger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.rust.lang.core.psi.RsElementTypes.*;

/**
 * The keys under which {@link RsImplMacroCallIndex} and {@link RsImplMacroDefIndex} find the macros that may expand to an
 * impl for a type, computed from the text of a macro body without expanding it.
 * <p>
 * A key is the name of a {@link TyFingerprint}: a type name ({@code Foo}, {@code u8}) or one of the shapes
 * ({@code (tuple)}, {@code [T]}, {@code fn()}, {@code #T} for an impl on a type parameter, ...). The self type of an
 * impl is the type after {@code impl<..>} and after {@code for}; in a macro body it may also be a metavariable, which
 * the arguments of the call fill in - so a call is found by the identifiers of its arguments too, under their own keys
 * ({@link #argument}), and such a call matters only when it calls a macro of {@link #METAVARIABLE_SELF_TYPE}.
 */
public final class RsImplMacroKeys {
    /**
     * The key of the calls and definitions that include a file ({@code include!}, or tonic's {@code include_proto!}): the
     * file can hold any impl, so every lookup expands them.
     */
    public static final String INCLUDE = "#include";

    /** The key of the definitions whose impl takes its self type from a metavariable: {@code impl $t}, {@code for $t}. */
    public static final String METAVARIABLE_SELF_TYPE = "#$";

    /** The key of the calls that name {@code identifier} in their arguments. */
    @Nonnull
    public static String argument(@Nonnull String identifier) {
        return "?" + identifier;
    }

    /** The key of the calls of the macro {@code name}. */
    @Nonnull
    public static String callOf(@Nonnull String name) {
        return "!" + name;
    }

    private RsImplMacroKeys() {
    }

    /** The keys a lookup of impls for {@code fingerprint} reads. */
    @Nonnull
    public static Set<String> forFingerprint(@Nonnull TyFingerprint fingerprint) {
        String name = fingerprint.getName();
        if (name.equals("{integer}")) return TyInteger.NAMES;
        if (name.equals("{float}")) return TyFloat.NAMES;
        return Set.of(name);
    }

    /** The identifiers of {@code body}: in the arguments of a call they may be the self type of an impl. */
    @Nonnull
    public static Set<String> identifiers(@Nonnull CharSequence body) {
        Set<String> result = new HashSet<>();
        for (Token token : tokens(body)) {
            if (token.type == IDENTIFIER) result.add(token.text);
        }
        return result;
    }

    /** The macros {@code body} calls: {@code name!}. */
    @Nonnull
    public static Set<String> calledMacros(@Nonnull CharSequence body) {
        List<Token> tokens = tokens(body);
        Set<String> result = new HashSet<>();
        for (int i = 0; i + 1 < tokens.size(); i++) {
            if (tokens.get(i).type == IDENTIFIER && tokens.get(i + 1).type == EXCL) result.add(tokens.get(i).text);
        }
        return result;
    }

    /** Whether {@code body} calls {@code include!}. */
    public static boolean includesFile(@Nonnull CharSequence body) {
        List<Token> tokens = tokens(body);
        for (int i = 0; i + 1 < tokens.size(); i++) {
            if (tokens.get(i).type == IDENTIFIER && tokens.get(i).text.equals("include") && tokens.get(i + 1).type == EXCL) {
                return true;
            }
        }
        return false;
    }

    /** The self types of the impls written in {@code body}, or an empty set when it has no {@code impl}. */
    @Nonnull
    public static Set<String> implSelfTypes(@Nonnull CharSequence body) {
        List<Token> tokens = tokens(body);
        Set<String> result = new HashSet<>();
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).type != IMPL) continue;
            Set<String> typeParameters = new HashSet<>();
            int afterImpl = skipGenerics(tokens, i + 1, typeParameters);
            // `impl Foo {` or `impl Trait for Foo {`: both types are taken, a trait name is a harmless extra key
            addSelfType(tokens, afterImpl, typeParameters, result);
            for (int j = afterImpl; j < tokens.size() && tokens.get(j).type != LBRACE && tokens.get(j).type != SEMICOLON; j++) {
                if (tokens.get(j).type == FOR) addSelfType(tokens, j + 1, typeParameters, result);
            }
        }
        return result;
    }

    /** Skips {@code <..>} at {@code index}, collecting the names it declares; returns the index after it. */
    private static int skipGenerics(@Nonnull List<Token> tokens, int index, @Nonnull Set<String> names) {
        if (index >= tokens.size() || tokens.get(index).type != LT) return index;
        int depth = 0;
        boolean expectName = true;
        for (int i = index; i < tokens.size(); i++) {
            IElementType type = tokens.get(i).type;
            if (type == LT) {
                depth++;
                expectName = depth == 1;
            }
            else if (type == GT) {
                if (--depth == 0) return i + 1;
            }
            else if (type == GTGT) {
                depth -= 2;
                if (depth <= 0) return i + 1;
            }
            else if (depth == 1 && type == COMMA) {
                expectName = true;
            }
            else if (expectName && depth == 1 && type == IDENTIFIER) {
                names.add(tokens.get(i).text);
                expectName = false;
            }
            else if (expectName && type != CONST) {
                expectName = false;
            }
        }
        return tokens.size();
    }

    private static void addSelfType(@Nonnull List<Token> tokens, int index, @Nonnull Set<String> typeParameters,
                                    @Nonnull Set<String> result) {
        int i = index;
        // `for<'a>` is a higher-ranked bound, not the `for` of a trait impl
        if (i < tokens.size() && tokens.get(i).type == LT) return;
        while (i < tokens.size() && (tokens.get(i).type == AND || tokens.get(i).type == ANDAND
            || tokens.get(i).type == QUOTE_IDENTIFIER || tokens.get(i).type == MUT)) {
            i++;
        }
        if (i >= tokens.size()) return;
        IElementType type = tokens.get(i).type;
        if (type == LPAREN) {
            result.add(i + 1 < tokens.size() && tokens.get(i + 1).type == RPAREN ? "()" : "(tuple)");
        }
        else if (type == LBRACK) {
            result.add("[T]");
            result.add("[T;]");
        }
        else if (type == MUL) {
            result.add("*T");
        }
        else if (type == FN || type == UNSAFE || type == EXTERN) {
            result.add("fn()");
        }
        else if (type == DYN) {
            result.add("dyn T");
        }
        else if (type == EXCL) {
            result.add("!");
        }
        else if (type == DOLLAR) {
            result.add(METAVARIABLE_SELF_TYPE);
        }
        else if (type == IDENTIFIER || type == COLONCOLON) {
            // the last segment of a path: `crate::io::Foo<T>` is `Foo`
            String last = null;
            while (i < tokens.size() && (tokens.get(i).type == IDENTIFIER || tokens.get(i).type == COLONCOLON)) {
                if (tokens.get(i).type == IDENTIFIER) last = tokens.get(i).text;
                i++;
            }
            if (last == null) return;
            result.add(typeParameters.contains(last) ? TyFingerprint.TYPE_PARAMETER_OR_MACRO_FINGERPRINT.getName() : last);
            if (TyInteger.NAMES.contains(last)) result.add("{integer}");
            if (TyFloat.NAMES.contains(last)) result.add("{float}");
        }
    }

    private record Token(@Nonnull IElementType type, @Nonnull String text) {
    }

    @Nonnull
    private static List<Token> tokens(@Nonnull CharSequence text) {
        List<Token> result = new ArrayList<>();
        Lexer lexer = new RsLexer();
        lexer.start(text);
        IElementType type;
        while ((type = lexer.getTokenType()) != null) {
            if (type != TokenType.WHITE_SPACE && !RsTokenSets.RS_COMMENTS.contains(type)) {
                result.add(new Token(type, text.subSequence(lexer.getTokenStart(), lexer.getTokenEnd()).toString()));
            }
            lexer.advance();
        }
        return result;
    }
}
