/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.doc.psi;

import consulo.document.util.TextRange;
import consulo.language.impl.ast.CompositeElement;
import consulo.language.impl.ast.TreeElement;
import consulo.language.impl.psi.LeafPsiElement;
import consulo.language.impl.psi.PsiWhiteSpaceImpl;
import consulo.language.util.CharTable;
import jakarta.annotation.Nonnull;
import org.intellij.markdown.MarkdownAstUtil;
import org.intellij.markdown.parser.LinkMap;
import org.rust.lang.doc.psi.impl.RsDocGapImpl;

/**
 * Converts a Markdown tree built over the undecorated text of a doc comment into the doc comment
 * AST.
 * <p>
 * The Markdown tree only covers the text with the comment decoration stripped, so every gap between
 * two Markdown nodes is filled with leaves taken from {@link RsDocTextMap}. As a result the produced
 * AST covers the raw token text completely, which is what the platform asserts after a lazy parse.
 */
final class RsDocMarkdownAstBuilder {
    private final RsDocTextMap textMap;
    private final CharTable charTable;
    private final LinkMap linkMap;

    /** Offset in the original text up to which leaves have already been inserted. */
    private int prevNodeEnd = 0;

    RsDocMarkdownAstBuilder(@Nonnull RsDocTextMap textMap, @Nonnull CharTable charTable, @Nonnull LinkMap linkMap) {
        this.textMap = textMap;
        this.charTable = charTable;
        this.linkMap = linkMap;
    }

    void buildTree(@Nonnull CompositeElement root, @Nonnull consulo.language.ast.ASTNode markdownRoot) {
        for (consulo.language.ast.ASTNode markdownChild : MarkdownAstUtil.children(markdownRoot)) {
            visitNode(root, markdownChild);
        }

        int originalLength = textMap.getOriginalText().length();
        if (prevNodeEnd < originalLength) {
            insertLeaves(root, prevNodeEnd, originalLength);
        }
    }

    private void visitNode(@Nonnull CompositeElement parent, @Nonnull consulo.language.ast.ASTNode markdownNode) {
        RsDocCompositeTokenType type = RsDocElementTypes.mapMarkdownToRust(markdownNode.getElementType());
        if (type == null) {
            // A `null` type means the node itself is not interesting, only its children are
            if (!MarkdownAstUtil.isLeaf(markdownNode)) {
                visitChildren(parent, markdownNode);
            }
            return;
        }

        insertLeavesUpTo(parent, markdownNode.getStartOffset());

        CompositeElement node = type.createCompositeNode();
        parent.rawAddChildrenWithoutNotifications(node);

        visitChildren(node, markdownNode);
        insertLeavesUpTo(node, MarkdownAstUtil.endOffset(markdownNode));
    }

    private void visitChildren(@Nonnull CompositeElement node, @Nonnull consulo.language.ast.ASTNode markdownNode) {
        if (node instanceof RsDocLinkDestination) {
            TextRange mappedRange = textMap.mapTextRangeToOriginal(textRangeOf(markdownNode));
            CharSequence mappedText = textMap.mapFully(mappedRange);
            if (mappedText != null) {
                node.rawAddChildrenWithoutNotifications(RsDocLinkDestinationParser.parse(mappedText, charTable));
                prevNodeEnd = mappedRange.getEndOffset();
                return;
            }
        }
        if (node instanceof RsDocLinkReferenceShort && tryParseShortLinkAsPath(node, markdownNode)) {
            return;
        }

        for (consulo.language.ast.ASTNode markdownChild : MarkdownAstUtil.children(markdownNode)) {
            visitNode(node, markdownChild);
        }
    }

    /**
     * <pre>
     * /// [bar1] - a direct link, refers to `bar1`, parsed as a path
     * /// [bar2] - a label, refers to `bar3`
     * ///
     * /// [bar2]: bar3
     * pub fn foo() {}
     * </pre>
     *
     * @return {@code true} if the link was consumed as a path
     */
    private boolean tryParseShortLinkAsPath(@Nonnull CompositeElement node,
                                            @Nonnull consulo.language.ast.ASTNode markdownNode) {
        TextRange mappedRange = textMap.mapTextRangeToOriginal(textRangeOf(markdownNode));
        CharSequence mappedText = textMap.mapFully(mappedRange);
        if (mappedText == null || mappedText.length() == 0) return false;
        if (mappedText.charAt(0) != '[' || mappedText.charAt(mappedText.length() - 1) != ']') return false;
        if (linkMap.getLinkInfo(mappedText) != null) return false;

        TreeElement linkNode = RsDocLinkDestinationParser.parseShortLink(mappedText, charTable);
        if (linkNode == null) return false;

        node.rawAddChildrenWithoutNotifications(linkNode);
        prevNodeEnd = mappedRange.getEndOffset();
        return true;
    }

    /**
     * Inserts the leaves covering {@code [startOffset, endOffset)} of the original text.
     */
    private void insertLeaves(@Nonnull CompositeElement parent, int startOffset, int endOffset) {
        textMap.processPiecesInRange(startOffset, endOffset, piece -> {
            CharSequence text = charTable.intern(piece.str);
            TreeElement element = switch (piece.kind) {
                case TEXT -> new LeafPsiElement(RsDocElementTypes.DOC_DATA, text);
                case GAP -> new RsDocGapImpl(RsDocElementTypes.DOC_GAP, text);
                case WHITESPACE -> new PsiWhiteSpaceImpl(text);
            };
            parent.rawAddChildrenWithoutNotifications(element);
        });
    }

    /**
     * Inserts the leaves covering everything between the last inserted leaf and the given offset
     * in the undecorated text.
     */
    private void insertLeavesUpTo(@Nonnull CompositeElement parent, int markdownEndOffset) {
        int endOffset = textMap.mapOffsetToOriginal(markdownEndOffset);
        if (endOffset != prevNodeEnd) {
            insertLeaves(parent, prevNodeEnd, endOffset);
        }
        prevNodeEnd = endOffset;
    }

    @Nonnull
    private static TextRange textRangeOf(@Nonnull consulo.language.ast.ASTNode markdownNode) {
        return new TextRange(markdownNode.getStartOffset(), MarkdownAstUtil.endOffset(markdownNode));
    }
}
