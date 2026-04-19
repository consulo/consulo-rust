/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.parser.RustParserDefinition;
import org.rust.lang.core.stubs.RsFileStub;

public class RsTokenType extends IElementType {

    public RsTokenType(@NotNull String debugName) {
        super(debugName, RsLanguage.INSTANCE);
    }

    @NotNull
    public static TokenSet tokenSetOf(@NotNull IElementType... tokens) {
        return TokenSet.create(tokens);
    }

    public static final TokenSet RS_KEYWORDS = tokenSetOf(
        RsElementTypes.AS, RsElementTypes.ASYNC, RsElementTypes.AUTO,
        RsElementTypes.BOX, RsElementTypes.BREAK,
        RsElementTypes.CONST, RsElementTypes.CONTINUE, RsElementTypes.CRATE, RsElementTypes.CSELF,
        RsElementTypes.DEFAULT, RsElementTypes.DYN,
        RsElementTypes.ELSE, RsElementTypes.ENUM, RsElementTypes.EXTERN,
        RsElementTypes.FN, RsElementTypes.FOR,
        RsElementTypes.IF, RsElementTypes.IMPL, RsElementTypes.IN,
        RsElementTypes.MACRO_KW,
        RsElementTypes.LET, RsElementTypes.LOOP,
        RsElementTypes.MATCH, RsElementTypes.MOD, RsElementTypes.MOVE, RsElementTypes.MUT,
        RsElementTypes.PUB,
        RsElementTypes.RAW, RsElementTypes.REF, RsElementTypes.RETURN,
        RsElementTypes.SELF, RsElementTypes.STATIC, RsElementTypes.STRUCT, RsElementTypes.SUPER,
        RsElementTypes.TRAIT, RsElementTypes.TYPE_KW,
        RsElementTypes.UNION, RsElementTypes.UNSAFE, RsElementTypes.USE,
        RsElementTypes.WHERE, RsElementTypes.WHILE,
        RsElementTypes.YIELD
    );

    public static final TokenSet RS_OPERATORS = tokenSetOf(
        RsElementTypes.AND, RsElementTypes.ANDEQ, RsElementTypes.ARROW, RsElementTypes.FAT_ARROW,
        RsElementTypes.SHA, RsElementTypes.COLON, RsElementTypes.COLONCOLON, RsElementTypes.COMMA,
        RsElementTypes.DIV, RsElementTypes.DIVEQ, RsElementTypes.DOT, RsElementTypes.DOTDOT,
        RsElementTypes.DOTDOTDOT, RsElementTypes.DOTDOTEQ, RsElementTypes.EQ, RsElementTypes.EQEQ,
        RsElementTypes.EXCL, RsElementTypes.EXCLEQ, RsElementTypes.GT, RsElementTypes.LT,
        RsElementTypes.MINUS, RsElementTypes.MINUSEQ, RsElementTypes.MUL, RsElementTypes.MULEQ,
        RsElementTypes.OR, RsElementTypes.OREQ, RsElementTypes.PLUS, RsElementTypes.PLUSEQ,
        RsElementTypes.REM, RsElementTypes.REMEQ, RsElementTypes.SEMICOLON, RsElementTypes.XOR,
        RsElementTypes.XOREQ, RsElementTypes.Q, RsElementTypes.AT, RsElementTypes.DOLLAR,
        RsElementTypes.GTGTEQ, RsElementTypes.GTGT, RsElementTypes.GTEQ, RsElementTypes.LTLTEQ,
        RsElementTypes.LTLT, RsElementTypes.LTEQ, RsElementTypes.OROR, RsElementTypes.ANDAND
    );

    public static final TokenSet RS_BINARY_OPS = tokenSetOf(
        RsElementTypes.AND, RsElementTypes.ANDEQ, RsElementTypes.ANDAND,
        RsElementTypes.DIV, RsElementTypes.DIVEQ,
        RsElementTypes.EQ, RsElementTypes.EQEQ, RsElementTypes.EXCLEQ,
        RsElementTypes.GT, RsElementTypes.GTGT, RsElementTypes.GTEQ, RsElementTypes.GTGTEQ,
        RsElementTypes.LT, RsElementTypes.LTLT, RsElementTypes.LTEQ, RsElementTypes.LTLTEQ,
        RsElementTypes.MINUS, RsElementTypes.MINUSEQ, RsElementTypes.MUL, RsElementTypes.MULEQ,
        RsElementTypes.OR, RsElementTypes.OREQ, RsElementTypes.OROR,
        RsElementTypes.PLUS, RsElementTypes.PLUSEQ,
        RsElementTypes.REM, RsElementTypes.REMEQ,
        RsElementTypes.XOR, RsElementTypes.XOREQ
    );

    public static final TokenSet RS_INNER_DOC_COMMENTS = tokenSetOf(
        RustParserDefinition.INNER_BLOCK_DOC_COMMENT, RustParserDefinition.INNER_EOL_DOC_COMMENT
    );

    public static final TokenSet RS_OUTER_DOC_COMMENTS = tokenSetOf(
        RustParserDefinition.OUTER_BLOCK_DOC_COMMENT, RustParserDefinition.OUTER_EOL_DOC_COMMENT
    );

    public static final TokenSet RS_DOC_COMMENTS = TokenSet.orSet(RS_INNER_DOC_COMMENTS, RS_OUTER_DOC_COMMENTS);

    public static final TokenSet RS_REGULAR_COMMENTS = tokenSetOf(
        RustParserDefinition.BLOCK_COMMENT, RustParserDefinition.EOL_COMMENT
    );

    public static final TokenSet RS_COMMENTS = TokenSet.orSet(RS_REGULAR_COMMENTS, RS_DOC_COMMENTS);

    public static final TokenSet RS_BLOCK_COMMENTS = tokenSetOf(
        RustParserDefinition.BLOCK_COMMENT, RustParserDefinition.INNER_BLOCK_DOC_COMMENT,
        RustParserDefinition.OUTER_BLOCK_DOC_COMMENT
    );

    public static final TokenSet RS_STRING_LITERALS = tokenSetOf(
        RsElementTypes.STRING_LITERAL, RsElementTypes.BYTE_STRING_LITERAL
    );

    public static final TokenSet RS_RAW_LITERALS = tokenSetOf(
        RsElementTypes.RAW_STRING_LITERAL, RsElementTypes.RAW_BYTE_STRING_LITERAL, RsElementTypes.RAW_CSTRING_LITERAL
    );

    public static final TokenSet RS_BYTE_STRING_LITERALS = tokenSetOf(
        RsElementTypes.BYTE_STRING_LITERAL, RsElementTypes.RAW_BYTE_STRING_LITERAL
    );

    public static final TokenSet RS_CSTRING_LITERALS = tokenSetOf(
        RsElementTypes.CSTRING_LITERAL, RsElementTypes.RAW_CSTRING_LITERAL
    );

    public static final TokenSet RS_ALL_STRING_LITERALS = tokenSetOf(
        RsElementTypes.STRING_LITERAL, RsElementTypes.BYTE_STRING_LITERAL, RsElementTypes.CSTRING_LITERAL,
        RsElementTypes.RAW_STRING_LITERAL, RsElementTypes.RAW_BYTE_STRING_LITERAL, RsElementTypes.RAW_CSTRING_LITERAL
    );

    public static final TokenSet RS_LITERALS = tokenSetOf(
        RsElementTypes.STRING_LITERAL, RsElementTypes.BYTE_STRING_LITERAL, RsElementTypes.CSTRING_LITERAL,
        RsElementTypes.RAW_STRING_LITERAL, RsElementTypes.RAW_BYTE_STRING_LITERAL, RsElementTypes.RAW_CSTRING_LITERAL,
        RsElementTypes.CHAR_LITERAL, RsElementTypes.BYTE_LITERAL, RsElementTypes.INTEGER_LITERAL,
        RsElementTypes.FLOAT_LITERAL, RsElementTypes.BOOL_LITERAL
    );

    public static final TokenSet RS_CONTEXTUAL_KEYWORDS = tokenSetOf(
        RsElementTypes.DEFAULT, RsElementTypes.UNION, RsElementTypes.AUTO, RsElementTypes.DYN, RsElementTypes.RAW
    );

    public static final TokenSet RS_EDITION_2018_KEYWORDS = tokenSetOf(
        RsElementTypes.ASYNC, RsElementTypes.TRY
    );

    public static final TokenSet RS_LIST_OPEN_SYMBOLS = tokenSetOf(RsElementTypes.LPAREN, RsElementTypes.LT);
    public static final TokenSet RS_LIST_CLOSE_SYMBOLS = tokenSetOf(RsElementTypes.RPAREN, RsElementTypes.GT);

    public static final TokenSet RS_BLOCK_LIKE_EXPRESSIONS = tokenSetOf(
        RsElementTypes.WHILE_EXPR, RsElementTypes.IF_EXPR, RsElementTypes.FOR_EXPR,
        RsElementTypes.LOOP_EXPR, RsElementTypes.MATCH_EXPR, RsElementTypes.BLOCK_EXPR
    );

    /** Successors of {@code org.rust.lang.core.psi.ext.RsItemElement} */
    public static final TokenSet RS_ITEMS = tokenSetOf(
        RsElementTypes.CONSTANT,
        RsElementTypes.ENUM_ITEM,
        RsElementTypes.EXTERN_CRATE_ITEM,
        RsElementTypes.FOREIGN_MOD_ITEM,
        RsElementTypes.FUNCTION,
        RsElementTypes.IMPL_ITEM,
        RsElementTypes.MACRO_2,
        RsElementTypes.MOD_DECL_ITEM,
        RsElementTypes.MOD_ITEM,
        RsElementTypes.STRUCT_ITEM,
        RsElementTypes.TRAIT_ALIAS,
        RsElementTypes.TRAIT_ITEM,
        RsElementTypes.TYPE_ALIAS,
        RsElementTypes.USE_ITEM
    );

    /** Successors of {@code org.rust.lang.core.psi.RsTypeReference} */
    public static final TokenSet RS_TYPES = tokenSetOf(
        RsElementTypes.ARRAY_TYPE,
        RsElementTypes.REF_LIKE_TYPE,
        RsElementTypes.FN_POINTER_TYPE,
        RsElementTypes.TUPLE_TYPE,
        RsElementTypes.PAREN_TYPE,
        RsElementTypes.TRAIT_TYPE,
        RsElementTypes.UNIT_TYPE,
        RsElementTypes.NEVER_TYPE,
        RsElementTypes.INFER_TYPE,
        RsElementTypes.PATH_TYPE,
        RsElementTypes.MACRO_TYPE,
        RsElementTypes.FOR_IN_TYPE
    );

    public static final TokenSet RS_MOD_OR_FILE = tokenSetOf(RsElementTypes.MOD_ITEM, RsFileStub.Type);

    /**
     * Some tokens that treated as keywords by our lexer,
     * but rustc's macro parser treats them as identifiers
     */
    public static final TokenSet RS_IDENTIFIER_TOKENS = TokenSet.orSet(
        tokenSetOf(RsElementTypes.IDENTIFIER, RsElementTypes.BOOL_LITERAL),
        RS_KEYWORDS
    );
}
