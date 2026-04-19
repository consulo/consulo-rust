/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.formatter.impl;

import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.rust.ide.formatter.RsAlignmentStrategy;
import org.rust.ide.formatter.blocks.RsFmtBlock;

import static org.rust.lang.core.psi.RsElementTypes.*;

public final class RsAlignmentUtil {

    private RsAlignmentUtil() {
    }

    @NotNull
    public static RsAlignmentStrategy getAlignmentStrategy(@NotNull RsFmtBlock block) {
        IElementType elementType = block.getNode().getElementType();

        if (elementType == TUPLE_EXPR || elementType == VALUE_ARGUMENT_LIST
            || RsFmtImplUtil.SPECIAL_MACRO_ARGS.contains(elementType) || elementType == USE_GROUP) {
            return RsAlignmentStrategy.wrap()
                .alignIf((child, parent, ctx) -> {
                    // Do not align if we have only one argument as this may lead to
                    // some quirks when that argument is tuple expr.
                    boolean result = true;
                    if (parent != null) {
                        ASTNode lBrace = parent.getFirstChildNode();
                        ASTNode rBrace = parent.getLastChildNode();
                        if (RsFmtImplUtil.isBlockDelim(lBrace, parent) && RsFmtImplUtil.isBlockDelim(rBrace, parent)) {
                            result = RsFmtImplUtil.treeNonWSPrev(child) != lBrace || RsFmtImplUtil.treeNonWSNext(child) != rBrace;
                        }
                    }
                    return result;
                })
                .alignIf((c, p, ctx) -> !RsFmtImplUtil.isBlockDelim(c, p))
                .alignIf(block.getCtx().getCommonSettings().ALIGN_MULTILINE_PARAMETERS_IN_CALLS);
        }

        if (elementType == TUPLE_TYPE || elementType == TUPLE_FIELDS) {
            return RsAlignmentStrategy.wrap()
                .alignIf((c, p, ctx) -> !RsFmtImplUtil.isBlockDelim(c, p))
                .alignIf(block.getCtx().getCommonSettings().ALIGN_MULTILINE_PARAMETERS);
        }

        if (elementType == VALUE_PARAMETER_LIST) {
            return RsAlignmentStrategy.shared()
                .alignIf((c, p, ctx) -> !RsFmtImplUtil.isBlockDelim(c, p))
                .alignIf(block.getCtx().getCommonSettings().ALIGN_MULTILINE_PARAMETERS);
        }

        if (RsFmtImplUtil.FN_DECLS.contains(elementType)) {
            return RsAlignmentStrategy.shared()
                .alignIf((c, p, ctx) ->
                    (c.getElementType() == RET_TYPE && block.getCtx().getRustSettings().ALIGN_RET_TYPE)
                        || (c.getElementType() == WHERE_CLAUSE && block.getCtx().getRustSettings().ALIGN_WHERE_CLAUSE)
                );
        }

        if (elementType == PAT_TUPLE_STRUCT) {
            return RsAlignmentStrategy.wrap()
                .alignIf((c, p, x) -> x.getMetLBrace() && !RsFmtImplUtil.isBlockDelim(c, p))
                .alignIf(block.getCtx().getCommonSettings().ALIGN_MULTILINE_PARAMETERS_IN_CALLS);
        }

        if (elementType == DOT_EXPR) {
            return RsAlignmentStrategy.shared()
                .alignIf(DOT) // DOT is synthetic's block representative
                .alignIf(block.getCtx().getCommonSettings().ALIGN_MULTILINE_CHAINED_METHODS);
        }

        if (elementType == WHERE_CLAUSE) {
            return RsAlignmentStrategy.wrap()
                .alignIf(WHERE_PRED)
                .alignIf(block.getCtx().getRustSettings().ALIGN_WHERE_BOUNDS);
        }

        if (elementType == TYPE_PARAMETER_LIST) {
            return RsAlignmentStrategy.wrap()
                .alignIf(TYPE_PARAMETER, LIFETIME_PARAMETER)
                .alignIf(block.getCtx().getRustSettings().ALIGN_TYPE_PARAMS);
        }

        if (elementType == FOR_LIFETIMES) {
            return RsAlignmentStrategy.wrap()
                .alignIf(LIFETIME_PARAMETER)
                .alignIf(block.getCtx().getRustSettings().ALIGN_TYPE_PARAMS);
        }

        return RsAlignmentStrategy.NullStrategy;
    }

    @NotNull
    public static RsAlignmentStrategy alignUnlessBlockDelim(@NotNull RsAlignmentStrategy strategy) {
        return strategy.alignIf((c, p, ctx) -> !RsFmtImplUtil.isBlockDelim(c, p));
    }
}
