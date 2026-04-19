/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros.decl;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.parser.GeneratedParserUtilBase;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.parser.RustParser;
import org.rust.lang.core.parser.RustParserUtil;
import org.rust.lang.core.parser.ParserUtil;
import org.rust.lang.core.psi.RsTokenType;
import org.rust.lang.core.psi.RsElementTypes;

import java.util.*;

public enum FragmentKind {
    Ident("ident"),
    Path("path"),
    Expr("expr"),
    Ty("ty"),
    Pat("pat"),
    PatParam("pat_param"),
    Stmt("stmt"),
    Block("block"),
    Item("item"),
    Meta("meta"),
    Tt("tt"),
    Vis("vis"),
    Literal("literal"),
    Lifetime("lifetime");

    private final String myKind;

    private static final Map<String, FragmentKind> FRAGMENT_KINDS;
    public static final Set<String> kinds;

    static {
        Map<String, FragmentKind> map = new HashMap<>();
        for (FragmentKind kind : values()) {
            map.put(kind.myKind, kind);
        }
        FRAGMENT_KINDS = Collections.unmodifiableMap(map);
        kinds = Collections.unmodifiableSet(FRAGMENT_KINDS.keySet());
    }

    FragmentKind(@NotNull String kind) {
        myKind = kind;
    }

    public boolean parse(@NotNull PsiBuilder builder) {
        ParserUtil.clearFrame(builder);
        switch (this) {
            case Ident: return consumeToken(builder, RsTokenType.RS_IDENTIFIER_TOKENS);
            case Path: return RustParser.TypePathGenericArgsNoTypeQual(builder, 0);
            case Expr: return RustParser.Expr(builder, 0, -1);
            case Ty: return RustParser.TypeReference(builder, 0);
            case Pat: return RustParserUtil.parseSimplePat(builder);
            case PatParam: return RustParserUtil.parseSimplePat(builder);
            case Stmt: return parseStatement(builder);
            case Block: return RustParserUtil.parseCodeBlockLazy(builder, 0);
            case Item: return RustParser.Item(builder, 0);
            case Meta: return RustParser.MetaItemWithoutTT(builder, 0);
            case Vis: return parseVis(builder);
            case Tt: return parseTT(builder);
            case Lifetime: return GeneratedParserUtilBase.consumeTokenFast(builder, RsElementTypes.QUOTE_IDENTIFIER);
            case Literal: return parseLiteral(builder);
            default: return false;
        }
    }

    private static boolean consumeToken(@NotNull PsiBuilder builder, @NotNull TokenSet tokenSet) {
        IElementType tokenType = builder.getTokenType();
        if (tokenType != null && tokenSet.contains(tokenType)) {
            builder.advanceLexer();
            return true;
        }
        return false;
    }

    private static boolean parseStatement(@NotNull PsiBuilder builder) {
        return RustParser.LetDecl(builder, 0) || RustParser.Expr(builder, 0, -1);
    }

    private static boolean parseVis(@NotNull PsiBuilder builder) {
        RustParser.Vis(builder, 0);
        return true; // Vis can be empty
    }

    private static boolean parseTT(@NotNull PsiBuilder builder) {
        return RustParserUtil.unpairedToken(builder, 0) || RustParserUtil.parseTokenTreeLazy(builder, 0, RsElementTypes.TT);
    }

    private static boolean parseLiteral(@NotNull PsiBuilder builder) {
        return ParserUtil.rollbackIfFalse(builder, () -> {
            boolean hasMinus = GeneratedParserUtilBase.consumeTokenFast(builder, RsElementTypes.MINUS);
            if (!hasMinus || builder.getTokenType() == RsElementTypes.INTEGER_LITERAL || builder.getTokenType() == RsElementTypes.FLOAT_LITERAL) {
                return RustParser.LitExprWithoutAttrs(builder, 0);
            } else {
                return false;
            }
        });
    }

    @Nullable
    public static FragmentKind fromString(@NotNull String s) {
        return FRAGMENT_KINDS.get(s);
    }
}
