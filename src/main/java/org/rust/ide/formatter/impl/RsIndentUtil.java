/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.formatter.impl;

import com.intellij.formatting.Indent;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.ide.formatter.RsFmtContext;
import org.rust.ide.formatter.blocks.RsFmtBlock;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.RsIfExpr;
import org.rust.lang.core.psi.RsMatchExpr;
import org.rust.lang.core.psi.RsStructLiteral;
import org.rust.lang.core.psi.ext.RsLooplikeExpr;
import org.rust.lang.doc.psi.RsDocElementTypes;

import static org.rust.lang.core.psi.RsElementTypes.*;

public final class RsIndentUtil {

    private RsIndentUtil() {
    }

    @Nullable
    public static Indent computeIndent(@NotNull RsFmtBlock block, @NotNull ASTNode child, @NotNull RsFmtContext childCtx) {
        ASTNode node = block.getNode();
        IElementType parentType = node.getElementType();
        PsiElement parentPsi = node.getPsi();
        IElementType childType = child.getElementType();
        PsiElement childPsi = child.getPsi();

        // fn moo(...)
        // -> ...
        // where ... {}
        // =>
        // fn moo(...)
        //     -> ...
        //     where ... {}
        if (childType == RET_TYPE
            || (childType == WHERE_CLAUSE && block.getCtx().getRustSettings().INDENT_WHERE_CLAUSE)) {
            return Indent.getNormalIndent();
        }

        // Indent blocks excluding braces
        if (RsFmtImplUtil.isDelimitedBlock(node)) {
            return getIndentIfNotDelim(child, node);
        }

        // Indent flat block contents, excluding closing brace
        if (RsFmtImplUtil.isFlatBlock(node)) {
            if (childCtx.getMetLBrace()) {
                return getIndentIfNotDelim(child, node);
            } else {
                return Indent.getNoneIndent();
            }
        }

        // Indent let declarations
        if (parentType == LET_DECL) {
            return Indent.getContinuationWithoutFirstIndent();
        }

        //     let _ =
        //     92;
        // =>
        //     let _ =>
        //         92;
        if (childPsi instanceof RsExpr && (parentType == MATCH_ARM || parentType == CONSTANT)) {
            return Indent.getNormalIndent();
        }

        // Indent if-expressions
        if (parentPsi instanceof RsIfExpr) {
            return Indent.getNoneIndent();
        }

        // Indent loop-expressions
        if (parentPsi instanceof RsLooplikeExpr) {
            return Indent.getNoneIndent();
        }

        // Indent match-expressions
        if (parentPsi instanceof RsMatchExpr) {
            return Indent.getNoneIndent();
        }

        // Indent struct literals
        if (parentPsi instanceof RsStructLiteral) {
            return Indent.getNoneIndent();
        }

        // Indent other expressions (chain calls, binary expressions, ...)
        if (parentPsi instanceof RsExpr) {
            return Indent.getContinuationWithoutFirstIndent();
        }

        // Where clause bounds
        if (childType == WHERE_PRED) {
            return Indent.getContinuationWithoutFirstIndent();
        }

        if (childType == RsDocElementTypes.DOC_GAP && child.getChars().length() > 0 && child.getChars().charAt(0) == '*') {
            return Indent.getSpaceIndent(1);
        }

        return Indent.getNoneIndent();
    }

    @NotNull
    private static Indent getIndentIfNotDelim(@NotNull ASTNode child, @NotNull ASTNode parent) {
        if (RsFmtImplUtil.isBlockDelim(child, parent)) {
            return Indent.getNoneIndent();
        } else {
            return Indent.getNormalIndent();
        }
    }
}
