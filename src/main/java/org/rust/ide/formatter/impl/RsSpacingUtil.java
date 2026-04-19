/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.formatter.impl;

import com.intellij.formatting.ASTBlock;
import com.intellij.formatting.Block;
import com.intellij.formatting.Spacing;
import com.intellij.formatting.SpacingBuilder;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.TokenType;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.impl.source.tree.TreeUtil;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.ide.formatter.RsFmtContext;
import org.rust.ide.formatter.settings.RsCodeStyleSettings;
import org.rust.lang.core.parser.RustParserDefinition;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.PsiElementUtil;
import org.rust.lang.core.psi.ext.RsItemElement;
import org.rust.lang.core.psi.ext.RsNamedElement;

import static org.rust.lang.core.psi.RsElementTypes.*;
import static org.rust.lang.core.psi.RsTokenType.tokenSetOf;

public final class RsSpacingUtil {

    private RsSpacingUtil() {
    }

    @NotNull
    public static SpacingBuilder createSpacingBuilder(@NotNull CommonCodeStyleSettings commonSettings,
                                                      @NotNull RsCodeStyleSettings rustSettings) {
        // Use sb1/sb2 temporaries to work around
        // https://youtrack.jetbrains.com/issue/KT-12239

        TokenSet ts_BLOCK_FIELDS_ENUM_BODY = tokenSetOf(BLOCK_FIELDS, ENUM_BODY);
        TokenSet ts_BLOCK_FIELDS_STRUCT_LITERAL_BODY = tokenSetOf(BLOCK_FIELDS, STRUCT_LITERAL_BODY);
        TokenSet ts_AND = tokenSetOf(AND);
        TokenSet ts_REF_LIKE_SELF_PAT_REF_PARAM = tokenSetOf(REF_LIKE_TYPE, SELF_PARAMETER, PAT_REF, VALUE_PARAMETER);
        TokenSet ts_SHA_EXCL_LBRACK_RBRACK = tokenSetOf(SHA, EXCL, LBRACK, RBRACK);
        TokenSet ts_LPAREN_RPAREN = tokenSetOf(LPAREN, RPAREN);
        TokenSet ts_IDENTIFIER_FN = tokenSetOf(IDENTIFIER, FN);
        TokenSet ts_TYPE_PARAMETER_LIST = tokenSetOf(TYPE_PARAMETER_LIST);
        TokenSet ts_MUL = tokenSetOf(MUL);
        TokenSet ts_CONST_MUT = tokenSetOf(CONST, MUT);
        TokenSet ts_Q = tokenSetOf(Q);
        TokenSet ts_BOUND = tokenSetOf(BOUND);

        SpacingBuilder sb1 = new SpacingBuilder(commonSettings)
            // Rules defined earlier have higher priority.
            // Beware of comments between blocks!

            //== some special operators
            // FIXME(mkaput): Doesn't work well with comments
            .afterInside(COMMA, ts_BLOCK_FIELDS_ENUM_BODY).parentDependentLFSpacing(1, 1, true, 1)
            .afterInside(COMMA, ts_BLOCK_FIELDS_STRUCT_LITERAL_BODY).parentDependentLFSpacing(1, 1, true, 1)
            .after(COMMA).spacing(1, 1, 0, true, 0)
            .before(COMMA).spaceIf(false)
            .after(COLON).spaceIf(true)
            .before(COLON).spaceIf(false)
            .after(SEMICOLON).spaceIf(true)
            .before(SEMICOLON).spaceIf(false)
            .afterInside(AND, ts_REF_LIKE_SELF_PAT_REF_PARAM).spaces(0)
            .beforeInside(Q, TRY_EXPR).spaces(0)
            .afterInside(RsFmtImplUtil.UNARY_OPS, UNARY_EXPR).spaces(0)
            // `use ::{bar}`
            .between(USE, COLONCOLON).spaces(1)

            //== attributes
            .aroundInside(ts_SHA_EXCL_LBRACK_RBRACK, RsFmtImplUtil.ATTRS).spaces(0)
            .aroundInside(ts_LPAREN_RPAREN, META_ITEM_ARGS).spaces(0)
            .around(META_ITEM_ARGS).spaces(0)

            //== empty parens
            .between(LPAREN, RPAREN).spacing(0, 0, 0, false, 0)
            .between(LBRACK, RBRACK).spacing(0, 0, 0, false, 0)
            .between(LBRACE, RBRACE).spacing(0, 0, 0, false, 0)
            .betweenInside(OR, OR, LAMBDA_EXPR).spacing(0, 0, 0, false, 0)

            //== paren delimited lists
            .afterInside(LPAREN, RsFmtImplUtil.PAREN_LISTS).spacing(0, 0, 0, true, 0)
            .beforeInside(RPAREN, RsFmtImplUtil.PAREN_LISTS).spacing(0, 0, 0, true, 0)
            .afterInside(LBRACK, RsFmtImplUtil.BRACK_LISTS).spacing(0, 0, 0, true, 0)
            .beforeInside(RBRACK, RsFmtImplUtil.BRACK_LISTS).spacing(0, 0, 0, true, 0)
            .afterInside(LBRACE, RsFmtImplUtil.BRACE_LISTS).spacing(0, 0, 0, true, 0)
            .beforeInside(RBRACE, RsFmtImplUtil.BRACE_LISTS).spacing(0, 0, 0, true, 0)
            .afterInside(LT, RsFmtImplUtil.ANGLE_LISTS).spacing(0, 0, 0, true, 0)
            .beforeInside(GT, RsFmtImplUtil.ANGLE_LISTS).spacing(0, 0, 0, true, 0)
            .aroundInside(OR, VALUE_PARAMETER_LIST).spacing(0, 0, 0, false, 0);

        SpacingBuilder sb2 = sb1
            //== items
            .before(VIS_RESTRICTION).spaces(0) // pub(crate)
            .after(VIS).spaces(1)
            .between(VALUE_PARAMETER_LIST, RET_TYPE).spacing(1, 1, 0, true, 0)
            .before(WHERE_CLAUSE).spacing(1, 1, 0, true, 0)
            .beforeInside(LBRACE, RsFmtImplUtil.FLAT_BRACE_BLOCKS).spaces(1)

            .between(ts_IDENTIFIER_FN, VALUE_PARAMETER_LIST).spaceIf(false)
            .between(IDENTIFIER, TUPLE_FIELDS).spaces(0)
            .between(IDENTIFIER, TYPE_PARAMETER_LIST).spaceIf(false)
            .between(IDENTIFIER, TYPE_ARGUMENT_LIST).spaceIf(false)
            .between(IDENTIFIER, VALUE_ARGUMENT_LIST).spaceIf(false)
            .between(TYPE_PARAMETER_LIST, VALUE_PARAMETER_LIST).spaceIf(false)
            .before(VALUE_ARGUMENT_LIST).spaceIf(false)

            .between(BINDING_MODE, IDENTIFIER).spaces(1)
            .between(IMPL, TYPE_PARAMETER_LIST).spaces(0)
            .afterInside(TYPE_PARAMETER_LIST, IMPL_ITEM).spaces(1)
            .betweenInside(ts_TYPE_PARAMETER_LIST, RsTokenType.RS_TYPES, IMPL_ITEM).spaces(1)

            // Handling blocks is pretty complicated. Do not tamper with
            // them too much and let rustfmt do all the pesky work.
            .afterInside(LBRACE, RsFmtImplUtil.BLOCK_LIKE).parentDependentLFSpacing(1, 1, true, 0)
            .beforeInside(RBRACE, RsFmtImplUtil.BLOCK_LIKE).parentDependentLFSpacing(1, 1, true, 0)
            .afterInside(LBRACE, RsFmtImplUtil.FLAT_BRACE_BLOCKS).parentDependentLFSpacing(1, 1, true, 0)
            .beforeInside(RBRACE, RsFmtImplUtil.FLAT_BRACE_BLOCKS).parentDependentLFSpacing(1, 1, true, 0)
            .withinPairInside(LBRACE, RBRACE, PAT_STRUCT).spacing(1, 1, 0, true, 0)

            .betweenInside(IDENTIFIER, ALIAS, EXTERN_CRATE_ITEM).spaces(1)

            .betweenInside(IDENTIFIER, TUPLE_FIELDS, ENUM_VARIANT).spaces(0)
            .betweenInside(IDENTIFIER, VARIANT_DISCRIMINANT, ENUM_VARIANT).spaces(1);

        int spacesAroundAssocTypeBinding = rustSettings.SPACE_AROUND_ASSOC_TYPE_BINDING ? 1 : 0;

        SpacingBuilder sb3 = sb2
            //== types
            .afterInside(LIFETIME, REF_LIKE_TYPE).spaceIf(true)
            .betweenInside(ts_MUL, ts_CONST_MUT, REF_LIKE_TYPE).spaces(0)
            .before(TYPE_PARAM_BOUNDS).spaces(0)
            .beforeInside(LPAREN, PATH).spaces(0)
            .after(TYPE_QUAL).spaces(0)
            .betweenInside(FOR, LT, FOR_LIFETIMES).spacing(0, 0, 0, true, 0)
            .around(FOR_LIFETIMES).spacing(1, 1, 0, true, 0)
            .aroundInside(EQ, ASSOC_TYPE_BINDING).spaces(spacesAroundAssocTypeBinding)

            //?Sized
            .betweenInside(ts_Q, ts_BOUND, POLYBOUND).spaces(0)

            //== expressions
            .beforeInside(LPAREN, PAT_TUPLE_STRUCT).spaces(0)
            .beforeInside(LBRACK, INDEX_EXPR).spaces(0)
            .afterInside(VALUE_PARAMETER_LIST, LAMBDA_EXPR).spacing(1, 1, 0, true, 1)
            .between(MATCH_ARM, MATCH_ARM).spacing(1, 1, rustSettings.ALLOW_ONE_LINE_MATCH ? 0 : 1, true, 1)
            .between(BLOCK, ELSE_BRANCH).spacing(1, 1, 0, false, 0)
            .betweenInside(ELSE, BLOCK, ELSE_BRANCH).spacing(1, 1, 0, false, 0)

            //== macros
            .beforeInside(EXCL, MACRO_CALL).spaces(0)
            .beforeInside(EXCL, MACRO).spaces(0)
            .afterInside(EXCL, MACRO).spaces(1)
            .betweenInside(IDENTIFIER, MACRO_BODY, MACRO).spaces(1)

            //== rules with very large area of application
            .around(RsFmtImplUtil.NO_SPACE_AROUND_OPS).spaces(0)
            .around(BINARY_OP).spaces(1)
            .around(RsFmtImplUtil.SPACE_AROUND_OPS).spaces(1)
            .around(RsTokenType.RS_KEYWORDS).spaces(1);

        // applyForEach(BLOCK_LIKE) { before(it).spaces(1) }
        for (IElementType tt : RsFmtImplUtil.BLOCK_LIKE.getTypes()) {
            sb3 = sb3.before(tt).spaces(1);
        }

        return sb3;
    }

    @Nullable
    public static Spacing computeSpacing(@NotNull Block parentBlock,
                                         @Nullable Block child1,
                                         @NotNull Block child2,
                                         @NotNull RsFmtContext ctx) {
        if (child1 instanceof ASTBlock && child2 instanceof ASTBlock) {
            SpacingContext spacingCtx = SpacingContext.create((ASTBlock) child1, (ASTBlock) child2, ctx);
            if (spacingCtx != null) {
                Spacing result = computeCustomSpacing(spacingCtx, child2);
                if (result != null) {
                    return result;
                }
            }
        }
        return ctx.getSpacingBuilder().getSpacing(parentBlock, child1, child2);
    }

    @Nullable
    private static Spacing computeCustomSpacing(@NotNull SpacingContext sc, @NotNull Block child2) {
        if (sc.myElementType2 == RustParserDefinition.EOL_COMMENT) {
            return Spacing.createKeepingFirstColumnSpacing(1, Integer.MAX_VALUE, true, sc.myCtx.getCommonSettings().KEEP_BLANK_LINES_IN_CODE);
        }

        // struct S {\n a: u32...
        if (sc.myElementType1 == LBRACE
            && ((ASTBlock) child2).getNode() != null
            && ((ASTBlock) child2).getNode().getTreeParent() != null
            && ((ASTBlock) child2).getNode().getTreeParent().getElementType() == BLOCK_FIELDS
            && sc.myPsi2.getParent() != null
            && sc.myPsi2.getParent().getParent() instanceof RsStructItem
            && sc.myElementType2 != RBRACE) {
            return Spacing.createSpacing(1, 1, 1, true, 0);
        }

        // #[attr]\n<comment>\n => #[attr] <comment>\n etc.
        if (sc.myPsi1 instanceof RsOuterAttr && sc.myPsi2 instanceof PsiComment) {
            return Spacing.createSpacing(1, 1, 0, true, 0);
        }

        // Determine spacing between macro invocation and its arguments
        if (sc.myParentPsi instanceof RsMacroCall && sc.myElementType1 == EXCL) {
            if (sc.myNode2.getChars().length() > 0 && sc.myNode2.getChars().charAt(0) == '{'
                || sc.myElementType2 == IDENTIFIER) {
                return Spacing.createSpacing(1, 1, 0, false, 0);
            } else {
                return Spacing.createSpacing(0, 0, 0, false, 0);
            }
        }

        // Ensure that each attribute is in separate line; comment aware
        if ((sc.myPsi1 instanceof RsOuterAttr && (sc.myPsi2 instanceof RsOuterAttr || sc.myPsi1.getParent() instanceof RsItemElement))
            || (sc.myPsi1 instanceof PsiComment && (sc.myPsi2 instanceof RsOuterAttr || PsiElementUtil.getPrevNonCommentSibling(sc.myPsi1) instanceof RsOuterAttr))) {
            return lineBreak(1, true, 0);
        }

        // Format blank lines between statements (or return expression)
        if (RsFmtImplUtil.isStmtOrMacro(sc.myNcPsi1) && RsFmtImplUtil.isStmtOrExpr(sc.myNcPsi2)) {
            return lineBreak(1,
                sc.myCtx.getCommonSettings().KEEP_LINE_BREAKS,
                sc.myCtx.getCommonSettings().KEEP_BLANK_LINES_IN_CODE);
        }

        // Format blank lines between impl & trait members
        if (sc.myParentPsi instanceof RsMembers && sc.myNcPsi1 instanceof RsNamedElement && sc.myNcPsi2 instanceof RsNamedElement) {
            return lineBreak(1,
                sc.myCtx.getCommonSettings().KEEP_LINE_BREAKS,
                sc.myCtx.getCommonSettings().KEEP_BLANK_LINES_IN_DECLARATIONS);
        }

        // Format blank lines between top level items
        if (RsFmtImplUtil.isTopLevelItem(sc.myNcPsi1) && RsFmtImplUtil.isTopLevelItem(sc.myNcPsi2)) {
            int minLineFeeds = 1 + (needsBlankLineBetweenItems(sc) ? sc.myCtx.getRustSettings().MIN_NUMBER_OF_BLANKS_BETWEEN_ITEMS : 0);
            return lineBreak(minLineFeeds,
                sc.myCtx.getCommonSettings().KEEP_LINE_BREAKS,
                sc.myCtx.getCommonSettings().KEEP_BLANK_LINES_IN_DECLARATIONS);
        }

        // Format blank lines between items (e.g. inside function)
        if (sc.myNcPsi1 instanceof RsItemElement
            && (sc.myNcPsi2 instanceof RsItemElement || RsFmtImplUtil.isStmtOrExpr(sc.myNcPsi2))
            && !(sc.myNode2.getFirstChildNode() instanceof PsiComment)) {
            return lineBreak(1, true, 1);
        }

        return null;
    }

    @NotNull
    private static Spacing lineBreak(int minLineFeeds, boolean keepLineBreaks, int keepBlankLines) {
        return Spacing.createSpacing(0, Integer.MAX_VALUE, minLineFeeds, keepLineBreaks, keepBlankLines);
    }

    private static boolean needsBlankLineBetweenItems(@NotNull SpacingContext sc) {
        if (RsTokenType.RS_COMMENTS.contains(sc.myElementType1) || RsTokenType.RS_COMMENTS.contains(sc.myElementType2)) {
            return false;
        }

        // Allow to keep consecutive runs of `use`, `const` or other "one line" items without blank lines
        if (sc.myElementType1 == sc.myElementType2 && RsFmtImplUtil.ONE_LINE_ITEMS.contains(sc.myElementType1)) {
            return false;
        }

        // #![deny(missing_docs)
        // extern crate regex;
        if (sc.myElementType1 == INNER_ATTR && sc.myElementType2 == EXTERN_CRATE_ITEM) {
            return false;
        }

        return true;
    }

    private static boolean hasLineBreakAfterInSameParent(@NotNull ASTNode node) {
        ASTNode next = node.getTreeNext();
        if (next == null) return false;
        ASTNode firstLeaf = TreeUtil.findFirstLeaf(next);
        return isWhiteSpaceWithLineBreak(firstLeaf);
    }

    private static boolean hasLineBreakBeforeInSameParent(@NotNull ASTNode node) {
        ASTNode prev = node.getTreePrev();
        if (prev == null) return false;
        ASTNode lastLeaf = TreeUtil.findLastLeaf(prev);
        return isWhiteSpaceWithLineBreak(lastLeaf);
    }

    private static boolean isWhiteSpaceWithLineBreak(@Nullable ASTNode node) {
        return node != null && node.getElementType() == TokenType.WHITE_SPACE && node.textContains('\n');
    }

    private static final class SpacingContext {
        final ASTNode myNode1;
        final ASTNode myNode2;
        final PsiElement myPsi1;
        final PsiElement myPsi2;
        final IElementType myElementType1;
        final IElementType myElementType2;
        final IElementType myParentType;
        final PsiElement myParentPsi;
        final PsiElement myNcPsi1;
        final PsiElement myNcPsi2;
        final RsFmtContext myCtx;

        SpacingContext(@NotNull ASTNode node1, @NotNull ASTNode node2,
                       @NotNull PsiElement psi1, @NotNull PsiElement psi2,
                       @NotNull IElementType elementType1, @NotNull IElementType elementType2,
                       @Nullable IElementType parentType, @Nullable PsiElement parentPsi,
                       @NotNull PsiElement ncPsi1, @NotNull PsiElement ncPsi2,
                       @NotNull RsFmtContext ctx) {
            myNode1 = node1;
            myNode2 = node2;
            myPsi1 = psi1;
            myPsi2 = psi2;
            myElementType1 = elementType1;
            myElementType2 = elementType2;
            myParentType = parentType;
            myParentPsi = parentPsi;
            myNcPsi1 = ncPsi1;
            myNcPsi2 = ncPsi2;
            myCtx = ctx;
        }

        @Nullable
        static SpacingContext create(@NotNull ASTBlock child1, @NotNull ASTBlock child2, @NotNull RsFmtContext ctx) {
            ASTNode node1 = child1.getNode();
            ASTNode node2 = child2.getNode();
            if (node1 == null || node2 == null) return null;
            PsiElement psi1 = node1.getPsi();
            PsiElement psi2 = node2.getPsi();
            IElementType elementType1 = psi1.getNode().getElementType();
            IElementType elementType2 = psi2.getNode().getElementType();
            IElementType parentType = node1.getTreeParent() != null ? node1.getTreeParent().getElementType() : null;
            PsiElement parentPsi = psi1.getParent();

            // Handle blocks of comments to get proper spacing between items and statements
            PsiElement ncPsi1;
            if (psi1 instanceof PsiComment && hasLineBreakAfterInSameParent(node1)) {
                PsiElement prev = PsiElementUtil.getPrevNonCommentSibling(psi1);
                ncPsi1 = prev != null ? prev : psi1;
            } else {
                ncPsi1 = psi1;
            }

            PsiElement ncPsi2;
            if (psi2 instanceof PsiComment && hasLineBreakBeforeInSameParent(node2)) {
                PsiElement next = PsiElementUtil.getNextNonCommentSibling(psi2);
                ncPsi2 = next != null ? next : psi2;
            } else {
                ncPsi2 = psi2;
            }

            return new SpacingContext(node1, node2, psi1, psi2, elementType1, elementType2,
                parentType, parentPsi, ncPsi1, ncPsi2, ctx);
        }
    }
}
