/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.doc.psi;

import org.intellij.markdown.IElementType;
import org.intellij.markdown.MarkdownElementTypes;
import org.intellij.markdown.MarkdownTokenTypes;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.doc.psi.impl.*;

import java.util.Set;

public final class RsDocElementTypes {
    public static final RsDocTokenType DOC_DATA = new RsDocTokenType("<DOC_DATA>");
    public static final RsDocTokenType DOC_GAP = new RsDocTokenType("<DOC_GAP>");

    public static final RsDocCompositeTokenType DOC_ATX_HEADING = new RsDocCompositeTokenType("<DOC_ATX_HEADING>", RsDocAtxHeadingImpl::new);
    public static final RsDocCompositeTokenType DOC_SETEXT_HEADING = new RsDocCompositeTokenType("<DOC_SETEXT_HEADING>", RsDocSetextHeadingImpl::new);

    public static final RsDocCompositeTokenType DOC_EMPHASIS = new RsDocCompositeTokenType("<DOC_EMPHASIS>", RsDocEmphasisImpl::new);
    public static final RsDocCompositeTokenType DOC_STRONG = new RsDocCompositeTokenType("<DOC_STRONG>", RsDocStrongImpl::new);
    public static final RsDocCompositeTokenType DOC_CODE_SPAN = new RsDocCompositeTokenType("<DOC_CODE_SPAN>", RsDocCodeSpanImpl::new);

    public static final RsDocCompositeTokenType DOC_AUTO_LINK = new RsDocCompositeTokenType("<DOC_AUTO_LINK>", RsDocAutoLinkImpl::new);
    public static final RsDocCompositeTokenType DOC_INLINE_LINK = new RsDocCompositeTokenType("<DOC_INLINE_LINK>", RsDocInlineLinkImpl::new);
    public static final RsDocCompositeTokenType DOC_SHORT_REFERENCE_LINK = new RsDocCompositeTokenType("<DOC_SHORT_REFERENCE_LINK>", RsDocLinkReferenceShortImpl::new);
    public static final RsDocCompositeTokenType DOC_FULL_REFERENCE_LINK = new RsDocCompositeTokenType("<DOC_FULL_REFERENCE_LINK>", RsDocLinkReferenceFullImpl::new);
    public static final RsDocCompositeTokenType DOC_LINK_DEFINITION = new RsDocCompositeTokenType("<DOC_LINK_DEFINITION>", RsDocLinkDefinitionImpl::new);

    public static final RsDocCompositeTokenType DOC_LINK_TEXT = new RsDocCompositeTokenType("<DOC_LINK_TEXT>", RsDocLinkTextImpl::new);
    public static final RsDocCompositeTokenType DOC_LINK_LABEL = new RsDocCompositeTokenType("<DOC_LINK_LABEL>", RsDocLinkLabelImpl::new);
    public static final RsDocCompositeTokenType DOC_LINK_TITLE = new RsDocCompositeTokenType("<DOC_LINK_TITLE>", RsDocLinkTitleImpl::new);
    public static final RsDocCompositeTokenType DOC_LINK_DESTINATION = new RsDocCompositeTokenType("<DOC_LINK_DESTINATION>", RsDocLinkDestinationImpl::new);

    public static final RsDocCompositeTokenType DOC_CODE_FENCE = new RsDocCompositeTokenType("<DOC_CODE_FENCE>", RsDocCodeFenceImpl::new);
    public static final RsDocCompositeTokenType DOC_CODE_BLOCK = new RsDocCompositeTokenType("<DOC_CODE_BLOCK>", RsDocCodeBlockImpl::new);
    public static final RsDocCompositeTokenType DOC_HTML_BLOCK = new RsDocCompositeTokenType("<DOC_HTML_BLOCK>", RsDocHtmlBlockImpl::new);

    public static final RsDocCompositeTokenType DOC_CODE_FENCE_START_END = new RsDocCompositeTokenType("<DOC_CODE_FENCE_START_END>", RsDocCodeFenceStartEndImpl::new);
    public static final RsDocCompositeTokenType DOC_CODE_FENCE_LANG = new RsDocCompositeTokenType("<DOC_CODE_FENCE_LANG>", RsDocCodeFenceLangImpl::new);

    private static final Set<IElementType> MARKDOWN_ATX_HEADINGS = Set.of(
        MarkdownElementTypes.ATX_1,
        MarkdownElementTypes.ATX_2,
        MarkdownElementTypes.ATX_3,
        MarkdownElementTypes.ATX_4,
        MarkdownElementTypes.ATX_5,
        MarkdownElementTypes.ATX_6
    );

    private RsDocElementTypes() {
    }

    /**
     * Some Markdown nodes are skipped (like {@code PARAGRAPH}) because they are mostly useless
     * for Rust and just increase Markdown tree depth. We're trying to keep the tree as simple as possible.
     */
    @Nullable
    public static RsDocCompositeTokenType mapMarkdownToRust(IElementType type) {
        if (MARKDOWN_ATX_HEADINGS.contains(type)) return DOC_ATX_HEADING;
        if (type == MarkdownElementTypes.SETEXT_1 || type == MarkdownElementTypes.SETEXT_2) return DOC_SETEXT_HEADING;
        if (type == MarkdownElementTypes.EMPH) return DOC_EMPHASIS;
        if (type == MarkdownElementTypes.STRONG) return DOC_STRONG;
        if (type == MarkdownElementTypes.CODE_SPAN) return DOC_CODE_SPAN;
        if (type == MarkdownElementTypes.AUTOLINK) return DOC_AUTO_LINK;
        if (type == MarkdownElementTypes.INLINE_LINK) return DOC_INLINE_LINK;
        if (type == MarkdownElementTypes.SHORT_REFERENCE_LINK) return DOC_SHORT_REFERENCE_LINK;
        if (type == MarkdownElementTypes.FULL_REFERENCE_LINK) return DOC_FULL_REFERENCE_LINK;
        if (type == MarkdownElementTypes.LINK_DEFINITION) return DOC_LINK_DEFINITION;
        if (type == MarkdownElementTypes.LINK_TEXT) return DOC_LINK_TEXT;
        if (type == MarkdownElementTypes.LINK_LABEL) return DOC_LINK_LABEL;
        if (type == MarkdownElementTypes.LINK_TITLE) return DOC_LINK_TITLE;
        if (type == MarkdownElementTypes.LINK_DESTINATION) return DOC_LINK_DESTINATION;
        if (type == MarkdownElementTypes.CODE_FENCE) return DOC_CODE_FENCE;
        if (type == MarkdownElementTypes.CODE_BLOCK) return DOC_CODE_BLOCK;
        if (type == MarkdownElementTypes.HTML_BLOCK) return DOC_HTML_BLOCK;
        if (type == MarkdownTokenTypes.CODE_FENCE_START || type == MarkdownTokenTypes.CODE_FENCE_END) return DOC_CODE_FENCE_START_END;
        if (type == MarkdownTokenTypes.FENCE_LANG) return DOC_CODE_FENCE_LANG;
        return null; // null means that the node is skipped
    }
}
