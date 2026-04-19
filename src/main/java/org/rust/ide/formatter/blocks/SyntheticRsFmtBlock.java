/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.formatter.blocks;

import com.intellij.formatting.*;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.ide.formatter.RsFmtContext;
import org.rust.ide.formatter.impl.RsSpacingUtil;

import java.util.List;

/**
 * Synthetic formatting block wraps a subsequence of sub blocks
 * and presents itself as one of the members of this subsequence.
 */
public class SyntheticRsFmtBlock implements ASTBlock {

    private final ASTBlock myRepresentative;
    private final List<Block> mySubBlocks;
    private final Alignment myAlignment;
    private final Indent myIndent;
    private final Wrap myWrap;
    private final RsFmtContext myCtx;
    private final TextRange myTextRange;

    public SyntheticRsFmtBlock(@Nullable ASTBlock representative,
                                @NotNull List<Block> subBlocks,
                                @Nullable Alignment alignment,
                                @Nullable Indent indent,
                                @Nullable Wrap wrap,
                                @NotNull RsFmtContext ctx) {
        assert !subBlocks.isEmpty() : "tried to build empty synthetic block";
        myRepresentative = representative;
        mySubBlocks = subBlocks;
        myAlignment = alignment;
        myIndent = indent;
        myWrap = wrap;
        myCtx = ctx;
        myTextRange = new TextRange(
            subBlocks.get(0).getTextRange().getStartOffset(),
            subBlocks.get(subBlocks.size() - 1).getTextRange().getEndOffset());
    }

    @NotNull
    @Override
    public TextRange getTextRange() {
        return myTextRange;
    }

    @Nullable
    @Override
    public ASTNode getNode() {
        return myRepresentative != null ? myRepresentative.getNode() : null;
    }

    @Nullable
    @Override
    public Alignment getAlignment() {
        if (myAlignment != null) return myAlignment;
        return myRepresentative != null ? myRepresentative.getAlignment() : null;
    }

    @Nullable
    @Override
    public Indent getIndent() {
        if (myIndent != null) return myIndent;
        return myRepresentative != null ? myRepresentative.getIndent() : null;
    }

    @Nullable
    @Override
    public Wrap getWrap() {
        if (myWrap != null) return myWrap;
        return myRepresentative != null ? myRepresentative.getWrap() : null;
    }

    @NotNull
    @Override
    public List<Block> getSubBlocks() {
        return mySubBlocks;
    }

    @NotNull
    @Override
    public ChildAttributes getChildAttributes(int newChildIndex) {
        return new ChildAttributes(myIndent, null);
    }

    @Nullable
    @Override
    public Spacing getSpacing(@Nullable Block child1, @NotNull Block child2) {
        return RsSpacingUtil.computeSpacing(this, child1, child2, myCtx);
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public boolean isIncomplete() {
        return mySubBlocks.get(mySubBlocks.size() - 1).isIncomplete();
    }

    @Override
    public String toString() {
        ASTNode firstNode = findFirstNonSyntheticChild();
        if (firstNode != null && firstNode.getPsi() != null && firstNode.getPsi().getContainingFile() != null) {
            String fileText = firstNode.getPsi().getContainingFile().getText();
            if (fileText != null) {
                String text = myTextRange.subSequence(fileText).toString();
                return text + " " + myTextRange;
            }
        }
        return "<rust synthetic> " + myTextRange;
    }

    @Nullable
    private ASTNode findFirstNonSyntheticChild() {
        Block child = mySubBlocks.get(0);
        if (child instanceof SyntheticRsFmtBlock) {
            return ((SyntheticRsFmtBlock) child).findFirstNonSyntheticChild();
        } else if (child instanceof ASTBlock) {
            return ((ASTBlock) child).getNode();
        }
        return null;
    }
}
