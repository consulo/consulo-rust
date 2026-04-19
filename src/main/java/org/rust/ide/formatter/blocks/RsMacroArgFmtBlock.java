/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.formatter.blocks;

import com.intellij.formatting.*;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.formatter.FormatterUtil;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.ide.formatter.RsFmtContext;
import org.rust.ide.formatter.RsFormattingModelBuilder;
import org.rust.ide.formatter.impl.RsFmtImplUtil;
import org.rust.lang.core.psi.MacroBraces;

import java.util.ArrayList;
import java.util.List;

import static org.rust.lang.core.psi.RsElementTypes.*;
import static org.rust.lang.core.psi.RsTokenType.tokenSetOf;

public class RsMacroArgFmtBlock implements ASTBlock {

    private static final TokenSet SUBTREES = tokenSetOf(MACRO_ARGUMENT, MACRO_ARGUMENT_TT, MACRO_BODY, MACRO_PATTERN, MACRO_EXPANSION);

    private final ASTNode myNode;
    private final Alignment myAlignment;
    private final Indent myIndent;
    private final Wrap myWrap;
    private final RsFmtContext myCtx;
    private List<Block> mySubBlocks;
    private Boolean myIsIncomplete;

    public RsMacroArgFmtBlock(@NotNull ASTNode node,
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
    @Override
    public List<Block> getSubBlocks() {
        if (mySubBlocks == null) {
            mySubBlocks = buildChildren();
        }
        return mySubBlocks;
    }

    @NotNull
    private List<Block> buildChildren() {
        ASTNode[] rawChildren = myNode.getChildren(null);
        List<Block> children = new ArrayList<>();

        for (ASTNode childNode : rawChildren) {
            if (RsFmtImplUtil.isWhitespaceOrEmpty(childNode)) continue;

            ASTBlock block = RsFormattingModelBuilder.createBlock(
                childNode,
                null,
                computeIndentForChild(childNode),
                null,
                myCtx);
            children.add(block);
        }

        return children;
    }

    @Nullable
    @Override
    public Spacing getSpacing(@Nullable Block child1, @NotNull Block child2) {
        return Spacing.getReadOnlySpacing();
    }

    @NotNull
    @Override
    public ChildAttributes getChildAttributes(int newChildIndex) {
        Indent indent;
        if (SUBTREES.contains(myNode.getElementType())) {
            indent = Indent.getNormalIndent();
        } else {
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

    @Nullable
    private Indent computeIndentForChild(@NotNull ASTNode child) {
        if (SUBTREES.contains(myNode.getElementType())) {
            if (MacroBraces.fromToken(child.getElementType()) == null) {
                return Indent.getNormalIndent();
            } else {
                return Indent.getNoneIndent();
            }
        }
        return Indent.getNoneIndent();
    }
}
