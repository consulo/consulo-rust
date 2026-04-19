/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.formatter.blocks;

import com.intellij.formatting.*;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.formatter.FormatterUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.ide.formatter.RsAlignmentStrategy;
import org.rust.ide.formatter.RsFmtContext;
import org.rust.ide.formatter.RsFormattingModelBuilder;
import org.rust.ide.formatter.impl.*;
import org.rust.lang.core.psi.RsExpr;

import java.util.ArrayList;
import java.util.List;

import static org.rust.lang.core.psi.RsElementTypes.*;

public class RsFmtBlock implements ASTBlock {

    private final ASTNode myNode;
    private final Alignment myAlignment;
    private final Indent myIndent;
    private final Wrap myWrap;
    private final RsFmtContext myCtx;
    private List<Block> mySubBlocks;
    private Boolean myIsIncomplete;

    public RsFmtBlock(@NotNull ASTNode node,
                       @Nullable Alignment alignment,
                       @Nullable Indent indent,
                       @Nullable Wrap wrap,
                       @NotNull RsFmtContext ctx) {
        myNode = node;
        myAlignment = alignment;
        myIndent = indent;
        myWrap = wrap;
        myCtx = ctx;
    }

    @NotNull
    @Override
    public ASTNode getNode() {
        return myNode;
    }

    @NotNull
    @Override
    public TextRange getTextRange() {
        return myNode.getTextRange();
    }

    @Nullable
    @Override
    public Alignment getAlignment() {
        return myAlignment;
    }

    @Nullable
    @Override
    public Indent getIndent() {
        return myIndent;
    }

    @Nullable
    @Override
    public Wrap getWrap() {
        return myWrap;
    }

    @NotNull
    public RsFmtContext getCtx() {
        return myCtx;
    }

    @NotNull
    @Override
    public List<Block> getSubBlocks() {
        if (mySubBlocks == null) {
            mySubBlocks = buildChildren();
        }
        return mySubBlocks;
    }

    @NotNull
    private List<Block> buildChildren() {
        Alignment sharedAlignment;
        if (RsFmtImplUtil.FN_DECLS.contains(myNode.getElementType())) {
            sharedAlignment = Alignment.createAlignment();
        } else if (myNode.getElementType() == VALUE_PARAMETER_LIST) {
            sharedAlignment = myCtx.getSharedAlignment();
        } else if (myNode.getElementType() == DOT_EXPR) {
            if (myNode.getTreeParent() != null && myNode.getTreeParent().getElementType() == DOT_EXPR) {
                sharedAlignment = myCtx.getSharedAlignment();
            } else {
                sharedAlignment = Alignment.createAlignment();
            }
        } else {
            sharedAlignment = null;
        }

        boolean metLBrace = false;
        RsAlignmentStrategy alignment = RsAlignmentUtil.getAlignmentStrategy(this);

        ASTNode[] rawChildren = myNode.getChildren(null);
        List<ASTBlock> children = new ArrayList<>();

        for (ASTNode childNode : rawChildren) {
            if (RsFmtImplUtil.isWhitespaceOrEmpty(childNode)) continue;

            if (RsFmtImplUtil.isFlatBlock(myNode) && RsFmtImplUtil.isBlockDelim(childNode, myNode)) {
                metLBrace = true;
            }

            RsFmtContext childCtx = myCtx.copy(sharedAlignment, metLBrace);

            ASTBlock block = RsFormattingModelBuilder.createBlock(
                childNode,
                alignment.getAlignment(childNode, myNode, childCtx),
                RsIndentUtil.computeIndent(this, childNode, childCtx),
                null,
                childCtx);
            children.add(block);
        }

        // Create fake `.sth` block here, so child indentation will
        // be relative to it when it starts from new line.
        // In other words: foo().bar().baz() => foo().baz()[.baz()]
        // We are using dot as our representative.
        if (myNode.getElementType() == DOT_EXPR) {
            int dotIndex = -1;
            for (int i = 0; i < children.size(); i++) {
                ASTNode childNode = children.get(i).getNode();
                if (childNode != null && childNode.getElementType() == DOT) {
                    dotIndex = i;
                    break;
                }
            }
            if (dotIndex != -1) {
                ASTBlock dotBlock = children.get(dotIndex);
                List<Block> syntheticSubBlocks = new ArrayList<>(children.subList(dotIndex, children.size()));
                SyntheticRsFmtBlock syntheticBlock = new SyntheticRsFmtBlock(
                    dotBlock, syntheticSubBlocks, null, null, null, myCtx);
                List<Block> result = new ArrayList<>(children.subList(0, dotIndex));
                result.add(syntheticBlock);
                return result;
            }
        }

        return new ArrayList<>(children);
    }

    @Nullable
    @Override
    public Spacing getSpacing(@Nullable Block child1, @NotNull Block child2) {
        return RsSpacingUtil.computeSpacing(this, child1, child2, myCtx);
    }

    @NotNull
    @Override
    public ChildAttributes getChildAttributes(int newChildIndex) {
        if (RsFmtImplUtil.CommaList.forElement(myNode.getElementType()) != null && newChildIndex > 1) {
            boolean isBeforeClosingBrace = newChildIndex + 1 == getSubBlocks().size();
            if (isBeforeClosingBrace) {
                return ChildAttributes.DELEGATE_TO_PREV_CHILD;
            } else {
                return ChildAttributes.DELEGATE_TO_NEXT_CHILD;
            }
        }

        Indent indent;
        if (RsFmtImplUtil.isFlatBraceBlock(myNode)) {
            // Flat brace blocks do not have separate PSI node for content blocks
            // so we have to manually decide whether new child is before (no indent)
            // or after (normal indent) left brace node.
            int lbraceIndex = -1;
            List<Block> blocks = getSubBlocks();
            for (int i = 0; i < blocks.size(); i++) {
                Block block = blocks.get(i);
                if (block instanceof ASTBlock) {
                    ASTNode node = ((ASTBlock) block).getNode();
                    if (node != null && node.getElementType() == LBRACE) {
                        lbraceIndex = i;
                        break;
                    }
                }
            }
            if (lbraceIndex != -1 && lbraceIndex < newChildIndex) {
                indent = Indent.getNormalIndent();
            } else {
                indent = Indent.getNoneIndent();
            }
        } else if (RsFmtImplUtil.isDelimitedBlock(myNode)) {
            // We are inside some kind of {...}, [...], (...) or <...> block
            indent = Indent.getNormalIndent();
        } else if (myNode.getPsi() instanceof RsExpr) {
            // Indent expressions (chain calls, binary expressions, ...)
            indent = Indent.getContinuationWithoutFirstIndent();
        } else {
            // Otherwise we don't want any indentation (null means continuation indent)
            indent = Indent.getNoneIndent();
        }
        return new ChildAttributes(indent, null);
    }

    @Override
    public boolean isLeaf() {
        return myNode.getFirstChildNode() == null;
    }

    @Override
    public boolean isIncomplete() {
        if (myIsIncomplete == null) {
            myIsIncomplete = FormatterUtil.isIncomplete(myNode);
        }
        return myIsIncomplete;
    }

    @Override
    public String toString() {
        return myNode.getText() + " " + getTextRange();
    }
}
