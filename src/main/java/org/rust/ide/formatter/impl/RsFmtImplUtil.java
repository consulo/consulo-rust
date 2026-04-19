/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.formatter.impl;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsAttr;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsItemElement;
import org.rust.lang.core.psi.ext.RsMod;

import java.util.List;
import java.util.function.Predicate;

import static org.rust.lang.core.psi.RsElementTypes.*;
import static org.rust.lang.core.psi.RsTokenType.tokenSetOf;

public final class RsFmtImplUtil {

    private RsFmtImplUtil() {
    }

    public static final TokenSet SPECIAL_MACRO_ARGS = tokenSetOf(
        FORMAT_MACRO_ARGUMENT, EXPR_MACRO_ARGUMENT, VEC_MACRO_ARGUMENT, ASSERT_MACRO_ARGUMENT, INCLUDE_MACRO_ARGUMENT
    );

    public static final TokenSet NO_SPACE_AROUND_OPS = tokenSetOf(COLONCOLON, DOT, DOTDOT, DOTDOTDOT, DOTDOTEQ);
    public static final TokenSet SPACE_AROUND_OPS = TokenSet.andNot(RsTokenType.RS_OPERATORS, NO_SPACE_AROUND_OPS);
    public static final TokenSet UNARY_OPS = tokenSetOf(MINUS, MUL, EXCL, AND, ANDAND);

    public static final TokenSet PAREN_DELIMITED_BLOCKS = TokenSet.orSet(
        tokenSetOf(VALUE_PARAMETER_LIST, PAREN_EXPR, TUPLE_EXPR, TUPLE_TYPE, VALUE_ARGUMENT_LIST, PAT_TUP, TUPLE_FIELDS, VIS_RESTRICTION),
        SPECIAL_MACRO_ARGS
    );
    public static final TokenSet PAREN_LISTS = TokenSet.orSet(PAREN_DELIMITED_BLOCKS, tokenSetOf(PAT_TUPLE_STRUCT));

    public static final TokenSet BRACK_DELIMITED_BLOCKS = TokenSet.orSet(tokenSetOf(ARRAY_TYPE, ARRAY_EXPR), SPECIAL_MACRO_ARGS);
    public static final TokenSet BRACK_LISTS = TokenSet.orSet(BRACK_DELIMITED_BLOCKS, tokenSetOf(INDEX_EXPR));

    public static final TokenSet BLOCK_LIKE = tokenSetOf(BLOCK, BLOCK_FIELDS, STRUCT_LITERAL_BODY, MATCH_BODY, ENUM_BODY, MEMBERS);
    public static final TokenSet BRACE_LISTS = TokenSet.orSet(tokenSetOf(USE_GROUP), SPECIAL_MACRO_ARGS);
    public static final TokenSet BRACE_DELIMITED_BLOCKS = TokenSet.orSet(BLOCK_LIKE, BRACE_LISTS);

    public static final TokenSet ANGLE_DELIMITED_BLOCKS = tokenSetOf(TYPE_PARAMETER_LIST, TYPE_ARGUMENT_LIST, FOR_LIFETIMES);
    public static final TokenSet ANGLE_LISTS = TokenSet.orSet(ANGLE_DELIMITED_BLOCKS, tokenSetOf(TYPE_QUAL));

    public static final TokenSet ATTRS = tokenSetOf(OUTER_ATTR, INNER_ATTR);
    public static final TokenSet MOD_LIKE_ITEMS = tokenSetOf(FOREIGN_MOD_ITEM, MOD_ITEM);

    public static final TokenSet DELIMITED_BLOCKS = TokenSet.orSet(
        BRACE_DELIMITED_BLOCKS, BRACK_DELIMITED_BLOCKS,
        PAREN_DELIMITED_BLOCKS, ANGLE_DELIMITED_BLOCKS
    );
    public static final TokenSet FLAT_BRACE_BLOCKS = TokenSet.orSet(MOD_LIKE_ITEMS, tokenSetOf(PAT_STRUCT));

    public static final TokenSet FN_DECLS = tokenSetOf(FUNCTION, FN_POINTER_TYPE, LAMBDA_EXPR);

    public static final TokenSet ONE_LINE_ITEMS = tokenSetOf(USE_ITEM, CONSTANT, MOD_DECL_ITEM, EXTERN_CRATE_ITEM, TYPE_ALIAS, INNER_ATTR);

    public static boolean isTopLevelItem(@NotNull PsiElement element) {
        return (element instanceof RsItemElement || element instanceof RsAttr) && element.getParent() instanceof RsMod;
    }

    public static boolean isStmtOrExpr(@NotNull PsiElement element) {
        return element instanceof RsStmt || element instanceof RsExpr || (element instanceof RsMacroCall && element.getParent() instanceof RsBlock);
    }

    public static boolean isStmtOrMacro(@NotNull PsiElement element) {
        return element instanceof RsStmt || (element instanceof RsMacroCall && element.getParent() instanceof RsBlock);
    }

    public static boolean isDelimitedBlock(@NotNull ASTNode node) {
        return DELIMITED_BLOCKS.contains(node.getElementType());
    }

    public static boolean isFlatBraceBlock(@NotNull ASTNode node) {
        return FLAT_BRACE_BLOCKS.contains(node.getElementType());
    }

    /**
     * A flat block is a Rust PSI element which does not denote separate PSI
     * element for its <em>block</em> part (e.g. {@code {...}}), for example {@code MOD_ITEM}.
     */
    public static boolean isFlatBlock(@NotNull ASTNode node) {
        return isFlatBraceBlock(node) || node.getElementType() == PAT_TUPLE_STRUCT;
    }

    public static boolean isBlockDelim(@NotNull ASTNode node, @Nullable ASTNode parent) {
        if (parent == null) return false;
        IElementType parentType = parent.getElementType();
        IElementType elementType = node.getElementType();

        if (elementType == LBRACE || elementType == RBRACE) {
            return BRACE_DELIMITED_BLOCKS.contains(parentType) || isFlatBraceBlock(parent);
        }
        if (elementType == LBRACK || elementType == RBRACK) {
            return BRACK_LISTS.contains(parentType);
        }
        if (elementType == LPAREN || elementType == RPAREN) {
            return PAREN_LISTS.contains(parentType) || parentType == PAT_TUPLE_STRUCT;
        }
        if (elementType == LT || elementType == GT) {
            return ANGLE_LISTS.contains(parentType);
        }
        if (elementType == OR) {
            ASTNode grandParent = parent.getTreeParent();
            return parentType == VALUE_PARAMETER_LIST && grandParent != null && grandParent.getElementType() == LAMBDA_EXPR;
        }
        return false;
    }

    public static boolean isWhitespaceOrEmpty(@Nullable ASTNode node) {
        return node == null || node.getTextLength() == 0 || node.getElementType() == TokenType.WHITE_SPACE;
    }

    @Nullable
    public static ASTNode treeNonWSPrev(@NotNull ASTNode node) {
        ASTNode current = node.getTreePrev();
        while (current != null && current.getElementType() == TokenType.WHITE_SPACE) {
            current = current.getTreePrev();
        }
        return current;
    }

    @Nullable
    public static ASTNode treeNonWSNext(@NotNull ASTNode node) {
        ASTNode current = node.getTreeNext();
        while (current != null && current.getElementType() == TokenType.WHITE_SPACE) {
            current = current.getTreeNext();
        }
        return current;
    }

    public static final class CommaList {
        private final IElementType myList;
        private final IElementType myClosingBrace;
        private final Predicate<PsiElement> myIsElement;

        public CommaList(@NotNull IElementType list, @NotNull IElementType closingBrace, @NotNull Predicate<PsiElement> isElement) {
            myList = list;
            myClosingBrace = closingBrace;
            myIsElement = isElement;
        }

        @NotNull
        public IElementType getList() {
            return myList;
        }

        @NotNull
        public IElementType getClosingBrace() {
            return myClosingBrace;
        }

        @NotNull
        public Predicate<PsiElement> getIsElement() {
            return myIsElement;
        }

        public boolean getNeedsSpaceBeforeClosingBrace() {
            return myClosingBrace == RBRACE && myList != USE_GROUP;
        }

        public boolean isElement(@NotNull PsiElement element) {
            return myIsElement.test(element);
        }

        @Override
        public String toString() {
            return "RsFmtImplUtil.CommaList(" + myList + ")";
        }

        @Nullable
        public static RsFmtImplUtil.CommaList forElement(@NotNull IElementType elementType) {
            for (RsFmtImplUtil.CommaList commaList : ALL) {
                if (commaList.myList == elementType) {
                    return commaList;
                }
            }
            return null;
        }

        private static final List<RsFmtImplUtil.CommaList> ALL = List.of(
            new RsFmtImplUtil.CommaList(BLOCK_FIELDS, RBRACE, e -> e.getNode().getElementType() == NAMED_FIELD_DECL),
            new RsFmtImplUtil.CommaList(STRUCT_LITERAL_BODY, RBRACE, e -> e.getNode().getElementType() == STRUCT_LITERAL_FIELD),
            new RsFmtImplUtil.CommaList(ENUM_BODY, RBRACE, e -> e.getNode().getElementType() == ENUM_VARIANT),
            new RsFmtImplUtil.CommaList(USE_GROUP, RBRACE, e -> e.getNode().getElementType() == USE_SPECK),

            new RsFmtImplUtil.CommaList(TUPLE_FIELDS, RPAREN, e -> e.getNode().getElementType() == TUPLE_FIELD_DECL),
            new RsFmtImplUtil.CommaList(VALUE_PARAMETER_LIST, RPAREN, e -> e.getNode().getElementType() == VALUE_PARAMETER),
            new RsFmtImplUtil.CommaList(VALUE_ARGUMENT_LIST, RPAREN, e -> e instanceof RsExpr),

            new RsFmtImplUtil.CommaList(VEC_MACRO_ARGUMENT, RBRACK, e -> e instanceof RsExpr)
        );
    }
}
