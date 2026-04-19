/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.formatter.blocks;

import com.intellij.formatting.*;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.formatter.common.AbstractBlock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Rust has multiline string literals, and whitespace inside them is actually significant.
 *
 * By default, the formatter will add and remove indentation <b>inside</b> string literals
 * without a moment of hesitation. To handle this situation, we create a separate FmtBlock
 * for each line of the string literal, and forbid messing with whitespace between them.
 */
public class RsMultilineStringLiteralBlock implements ASTBlock {

    private final ASTNode myNode;
    private final Alignment myAlignment;
    private final Indent myIndent;
    private final Wrap myWrap;

    public RsMultilineStringLiteralBlock(@NotNull ASTNode node,
                                          @Nullable Alignment alignment,
                                          @Nullable Indent indent,
                                          @Nullable Wrap wrap) {
        myNode = node;
        myAlignment = alignment;
        myIndent = indent;
        myWrap = wrap;
    }

    @NotNull
    @Override
    public ASTNode getNode() {
        return myNode;
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
    @Override
    public TextRange getTextRange() {
        return myNode.getTextRange();
    }

    @Override
    public boolean isIncomplete() {
        return false;
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @NotNull
    @Override
    public ChildAttributes getChildAttributes(int newChildIndex) {
        return new ChildAttributes(null, null);
    }

    @NotNull
    @Override
    public List<Block> getSubBlocks() {
        List<Block> result = new ArrayList<>();
        int startOffset = 0;
        CharSequence chars = myNode.getChars();

        for (int idx = 0; idx < chars.length(); idx++) {
            char c = chars.charAt(idx);
            if (c == '\n') {
                result.add(new RsLineBlock(myNode, new TextRange(startOffset, idx)));
                startOffset = idx;
            }
        }

        if (startOffset < chars.length()) {
            result.add(new RsLineBlock(myNode, new TextRange(startOffset, chars.length())));
        }
        return result;
    }

    @NotNull
    @Override
    public Spacing getSpacing(@Nullable Block child1, @NotNull Block child2) {
        return Spacing.getReadOnlySpacing();
    }

    private static class RsLineBlock extends AbstractBlock {
        private final TextRange myTextRange;

        RsLineBlock(@NotNull ASTNode node, @NotNull TextRange rangeInParent) {
            super(node, null, null);
            myTextRange = rangeInParent.shiftRight(node.getStartOffset());
        }

        @NotNull
        @Override
        public TextRange getTextRange() {
            return myTextRange;
        }

        @Override
        public boolean isLeaf() {
            return true;
        }

        @NotNull
        @Override
        protected List<Block> buildChildren() {
            return Collections.emptyList();
        }

        @Nullable
        @Override
        public Spacing getSpacing(@Nullable Block child1, @NotNull Block child2) {
            return null;
        }

        @Override
        public String toString() {
            TextRange rangeInNode = myTextRange.shiftRight(-myNode.getStartOffset());
            CharSequence chars = myNode.getChars();
            String text = chars.subSequence(rangeInNode.getStartOffset(), rangeInNode.getEndOffset()).toString();
            return '"' + text.replace("\n", "\\n").replace("\"", "\\\"") + '"';
        }
    }
}
