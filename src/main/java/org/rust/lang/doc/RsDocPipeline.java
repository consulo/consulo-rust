/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.doc;

import com.intellij.codeEditor.printing.HTMLTextPainter;
import com.intellij.codeInsight.documentation.DocumentationManagerProtocol;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.ui.ColorHexUtil;
import com.intellij.ui.ColorUtil;
import org.intellij.markdown.IElementType;
import org.intellij.markdown.MarkdownElementTypes;
import org.intellij.markdown.MarkdownTokenTypes;
import org.intellij.markdown.ast.ASTNode;
import org.intellij.markdown.flavours.MarkdownFlavourDescriptor;
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor;
import org.intellij.markdown.html.*;
import org.intellij.markdown.html.entities.EntityConverter;
import org.intellij.markdown.parser.LinkMap;
import org.intellij.markdown.parser.MarkdownParser;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.types.ty.TyPrimitive;
import org.rust.lang.doc.psi.RsDocComment;
import org.rust.lang.doc.psi.RsDocKind;

import java.awt.*;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.rust.cargo.util.AutoInjectedCrates.STD;
import org.rust.lang.doc.psi.RsQualifiedName;

public final class RsDocPipeline {

    private RsDocPipeline() {
    }

    @NotNull
    public static String documentation(@NotNull RsDocAndAttributeOwner owner, boolean withInner) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (PsiElement element : RsDocAndAttributeOwnerUtil.docElements(owner, withInner)) {
            if (element instanceof RsAttr) {
                RsAttr attr = (RsAttr) element;
                if (isDocAttr(attr)) {
                    String text = getDocAttr(attr);
                    if (text != null) {
                        RsDocKind kind = RsDocKind.Attr;
                        for (CharSequence line : (Iterable<CharSequence>) () -> kind.removeDecoration(text).iterator()) {
                            if (!first) sb.append("\n");
                            sb.append(line);
                            first = false;
                        }
                    }
                }
            } else if (element instanceof RsDocComment) {
                RsDocComment doc = (RsDocComment) element;
                RsDocKind kind = RsDocKind.of(doc.getTokenType());
                for (CharSequence line : (Iterable<CharSequence>) () -> kind.removeDecoration(doc.getText()).iterator()) {
                    if (!first) sb.append("\n");
                    sb.append(line);
                    first = false;
                }
            }
        }
        return sb.toString();
    }

    @NotNull
    public static String documentation(@NotNull RsDocAndAttributeOwner owner) {
        return documentation(owner, true);
    }

    @Nullable
    @Nls
    public static String documentationAsHtml(@NotNull RsDocAndAttributeOwner owner) {
        return documentationAsHtml(owner, owner, RsDocRenderMode.QUICK_DOC_POPUP);
    }

    @Nullable
    @Nls
    public static String documentationAsHtml(@NotNull RsDocAndAttributeOwner owner,
                                              @NotNull PsiElement originalElement) {
        return documentationAsHtml(owner, originalElement, RsDocRenderMode.QUICK_DOC_POPUP);
    }

    @Nullable
    @Nls
    public static String documentationAsHtml(@NotNull RsDocAndAttributeOwner owner,
                                              @NotNull PsiElement originalElement,
                                              @NotNull RsDocRenderMode renderMode) {
        return documentationAsHtmlInternal(documentation(owner), originalElement, renderMode);
    }

    @Nullable
    @Nls
    public static String documentationAsHtml(@NotNull RsDocComment comment, @NotNull RsDocRenderMode renderMode) {
        RsDocAndAttributeOwner owner = comment.getOwner();
        if (owner == null) return null;
        RsDocKind kind = RsDocKind.of(comment.getTokenType());
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (CharSequence line : (Iterable<CharSequence>) () -> kind.removeDecoration(comment.getText()).iterator()) {
            if (!first) sb.append("\n");
            sb.append(line);
            first = false;
        }
        return documentationAsHtmlInternal(sb.toString(), owner, renderMode);
    }

    @Nullable
    private static String documentationAsHtmlInternal(@NotNull String rawDocumentationText,
                                                       @NotNull PsiElement context,
                                                       @NotNull RsDocRenderMode renderMode) {
        String tmpUriPrefix = "psi://element/";
        String path;
        if (context instanceof RsQualifiedNamedElement) {
            RsQualifiedName qn = RsQualifiedName.from((RsQualifiedNamedElement) context);
            path = qn != null ? qn.toUrlPath() : null;
        } else if (context instanceof RsPath) {
            path = TyPrimitive.fromPath((RsPath) context) != null ? STD + "/" : null;
        } else if (RsElementUtil.isKeywordLike(context)) {
            path = STD + "/";
        } else {
            return null;
        }

        URI baseURI = null;
        if (path != null) {
            try {
                baseURI = URI.create(tmpUriPrefix + path);
            } catch (Exception e) {
                // ignore
            }
        }

        GFMFlavourDescriptor gfm = new GFMFlavourDescriptor(false, true, false);
        MarkdownFlavourDescriptor flavour = new RustDocMarkdownFlavourDescriptor(context, baseURI, renderMode, gfm);
        ASTNode root = new MarkdownParser(flavour).buildMarkdownTreeFromString(rawDocumentationText);
        return new HtmlGenerator(rawDocumentationText, root, flavour, false)
            .generateHtml(new HtmlGenerator.DefaultTagRenderer(HtmlGeneratorKt.getDUMMY_ATTRIBUTES_CUSTOMIZER(), false))
            .replace(tmpUriPrefix, DocumentationManagerProtocol.PSI_ELEMENT_PROTOCOL);
    }

    private static boolean isDocAttr(@NotNull RsAttr attr) {
        return "doc".equals(attr.getMetaItem().getName());
    }

    @Nullable
    private static String getDocAttr(@NotNull RsAttr attr) {
        if (!isDocAttr(attr)) return null;
        RsLitExpr litExpr = attr.getMetaItem().getLitExpr();
        return litExpr != null ? RsLitExprUtil.getStringValue(litExpr) : null;
    }

    private static boolean linkIsProbablyValidRustPath(@NotNull CharSequence link) {
        for (int i = 0; i < link.length(); i++) {
            char c = link.charAt(i);
            if (c == '/' || c == '.' || c == '#' || Character.isWhitespace(c)) {
                return false;
            }
        }
        return true;
    }

    @NotNull
    private static CharSequence markLinkAsLanguageItemIfItIsRustPath(@NotNull CharSequence link) {
        if (linkIsProbablyValidRustPath(link)) {
            return DocumentationManagerProtocol.PSI_ELEMENT_PROTOCOL + link;
        }
        return link;
    }

    // Inner classes for markdown rendering

    private static class RustDocMarkdownFlavourDescriptor implements MarkdownFlavourDescriptor {
        private final PsiElement context;
        private final URI uri;
        private final RsDocRenderMode renderMode;
        private final MarkdownFlavourDescriptor gfm;

        RustDocMarkdownFlavourDescriptor(PsiElement context, URI uri, RsDocRenderMode renderMode, MarkdownFlavourDescriptor gfm) {
            this.context = context;
            this.uri = uri;
            this.renderMode = renderMode;
            this.gfm = gfm;
        }

        @Override
        public Map<IElementType, GeneratingProvider> createHtmlGeneratingProviders(@NotNull LinkMap linkMap, @Nullable URI baseURI) {
            HashMap<IElementType, GeneratingProvider> providers = new HashMap<>(gfm.createHtmlGeneratingProviders(linkMap, uri != null ? uri : baseURI));
            providers.remove(MarkdownElementTypes.MARKDOWN_FILE);
            providers.put(MarkdownElementTypes.ATX_1, new SimpleTagProvider("h2"));
            providers.put(MarkdownElementTypes.ATX_2, new SimpleTagProvider("h3"));
            providers.put(MarkdownElementTypes.CODE_FENCE, new RsCodeFenceProvider(context, renderMode));

            URI resolvedUri = uri != null ? uri : baseURI;
            providers.put(MarkdownElementTypes.SHORT_REFERENCE_LINK,
                new RsReferenceLinksGeneratingProvider(linkMap, resolvedUri, true));
            providers.put(MarkdownElementTypes.FULL_REFERENCE_LINK,
                new RsReferenceLinksGeneratingProvider(linkMap, resolvedUri, true));
            providers.put(MarkdownElementTypes.INLINE_LINK,
                new RsInlineLinkGeneratingProvider(resolvedUri, true));

            return providers;
        }

        // Delegate remaining methods to gfm
        @Override
        public org.intellij.markdown.parser.MarkerProcessorFactory getMarkerProcessorFactory() {
            return gfm.getMarkerProcessorFactory();
        }

        @Override
        public org.intellij.markdown.parser.sequentialparsers.SequentialParserManager getSequentialParserManager() {
            return gfm.getSequentialParserManager();
        }

        @Override
        public org.intellij.markdown.lexer.MarkdownLexer createInlinesLexer() {
            return gfm.createInlinesLexer();
        }
    }

    private static class RsCodeFenceProvider implements GeneratingProvider {
        private static final Pattern COLOR_PATTERN = Pattern.compile("color:\\s*#(\\p{XDigit}{3,})");
        private static final int CODE_SNIPPET_INDENT = 20;
        private static final double LIGHT_THEME_ALPHA = 0.6;
        private static final double DARK_THEME_ALPHA = 0.78;

        private final PsiElement context;
        private final RsDocRenderMode renderMode;

        RsCodeFenceProvider(PsiElement context, RsDocRenderMode renderMode) {
            this.context = context;
            this.renderMode = renderMode;
        }

        @Override
        public void processNode(@NotNull HtmlGenerator.HtmlGeneratingVisitor visitor, @NotNull String text, @NotNull ASTNode node) {
            CharSequence nodeText = text.subSequence(node.getStartOffset(), node.getEndOffset());
            int indentBefore = 0;
            String spaces = "          ";
            for (int i = 0; i < Math.min(nodeText.length(), spaces.length()); i++) {
                if (nodeText.charAt(i) == ' ') indentBefore++;
                else break;
            }

            StringBuilder codeText = new StringBuilder();
            List<ASTNode> childrenToConsider = node.getChildren();
            if (!childrenToConsider.isEmpty() && childrenToConsider.get(childrenToConsider.size() - 1).getType() == MarkdownTokenTypes.CODE_FENCE_END) {
                childrenToConsider = childrenToConsider.subList(0, childrenToConsider.size() - 1);
            }

            boolean isContentStarted = false;
            boolean skipNextEOL = false;
            boolean lastChildWasContent = false;

            for (ASTNode child : childrenToConsider) {
                if (isContentStarted && (child.getType() == MarkdownTokenTypes.CODE_FENCE_CONTENT || child.getType() == MarkdownTokenTypes.EOL)) {
                    if (skipNextEOL && child.getType() == MarkdownTokenTypes.EOL) {
                        skipNextEOL = false;
                        continue;
                    }
                    String rawLine = HtmlGenerator.Companion.trimIndents(text.subSequence(child.getStartOffset(), child.getEndOffset()), indentBefore).toString();
                    String trimmedLine = rawLine.stripLeading();
                    if (trimmedLine.startsWith("#") && (trimmedLine.length() <= 1 || trimmedLine.charAt(1) == ' ')) {
                        skipNextEOL = true;
                        continue;
                    }
                    String codeLine;
                    if (trimmedLine.startsWith("##")) {
                        codeLine = trimmedLine.substring(1);
                    } else {
                        codeLine = rawLine;
                    }
                    codeText.append(codeLine);
                    lastChildWasContent = child.getType() == MarkdownTokenTypes.CODE_FENCE_CONTENT;
                }
                if (!isContentStarted && child.getType() == MarkdownTokenTypes.EOL) {
                    isContentStarted = true;
                }
            }
            if (lastChildWasContent) {
                codeText.append("\n");
            }

            visitor.consumeHtml(convertToHtmlWithHighlighting(codeText.toString()));
        }

        private String convertToHtmlWithHighlighting(String codeText) {
            String htmlCodeText = HTMLTextPainter.convertCodeFragmentToHTMLFragmentWithInlineStyles(context, codeText);

            EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
            htmlCodeText = htmlCodeText.replaceFirst("<pre>",
                "<pre style=\"text-indent: " + CODE_SNIPPET_INDENT + "px;\">");

            if (renderMode == RsDocRenderMode.INLINE_DOC_COMMENT) {
                htmlCodeText = dimColors(htmlCodeText, scheme);
            }
            return htmlCodeText;
        }

        private String dimColors(String html, EditorColorsScheme scheme) {
            double alpha = ColorUtil.isDark(scheme.getDefaultBackground()) ? DARK_THEME_ALPHA : LIGHT_THEME_ALPHA;

            Matcher matcher = COLOR_PATTERN.matcher(html);
            StringBuilder sb = new StringBuilder();
            while (matcher.find()) {
                String colorHexValue = matcher.group(1);
                Color fgColor = ColorHexUtil.fromHexOrNull(colorHexValue);
                if (fgColor == null) {
                    matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group()));
                    continue;
                }
                Color bgColor = scheme.getDefaultBackground();
                Color finalColor = ColorUtil.mix(bgColor, fgColor, alpha);
                matcher.appendReplacement(sb, Matcher.quoteReplacement("color: #" + ColorUtil.toHex(finalColor)));
            }
            matcher.appendTail(sb);
            return sb.toString();
        }
    }

    public static class RsReferenceLinksGeneratingProvider extends ReferenceLinksGeneratingProvider {
        private final LinkMap linkMap;

        public RsReferenceLinksGeneratingProvider(LinkMap linkMap, URI baseURI, boolean resolveAnchors) {
            super(linkMap, baseURI, resolveAnchors);
            this.linkMap = linkMap;
        }

        @Override
        public void renderLink(@NotNull HtmlGenerator.HtmlGeneratingVisitor visitor, @NotNull String text,
                               @NotNull ASTNode node, @NotNull RenderInfo info) {
            super.renderLink(visitor, text, node, info.copy(info.getLabel(), markLinkAsLanguageItemIfItIsRustPath(info.getDestination()),
                info.getTitle()));
        }

        @Override
        @Nullable
        public RenderInfo getRenderInfo(@NotNull String text, @NotNull ASTNode node) {
            ASTNode label = null;
            for (ASTNode child : node.getChildren()) {
                if (child.getType() == MarkdownElementTypes.LINK_LABEL) {
                    label = child;
                    break;
                }
            }
            if (label == null) return null;

            CharSequence labelText = text.subSequence(label.getStartOffset(), label.getEndOffset());
            LinkMap.LinkInfo linkInfo = linkMap.getLinkInfo(labelText);

            CharSequence linkDestination;
            CharSequence linkTitle;
            if (linkInfo != null) {
                linkDestination = linkInfo.getDestination();
                linkTitle = linkInfo.getTitle();
            } else {
                String linkText = labelText.toString();
                if (linkText.startsWith("[") && linkText.endsWith("]")) {
                    linkText = linkText.substring(1, linkText.length() - 1);
                }
                if (linkText.startsWith("`") && linkText.endsWith("`")) {
                    linkText = linkText.substring(1, linkText.length() - 1);
                }
                if (!linkIsProbablyValidRustPath(linkText)) return null;
                linkDestination = linkText;
                linkTitle = null;
            }

            ASTNode linkTextNode = null;
            for (ASTNode child : node.getChildren()) {
                if (child.getType() == MarkdownElementTypes.LINK_TEXT) {
                    linkTextNode = child;
                    break;
                }
            }

            EntityConverter entityConverter = EntityConverter.INSTANCE;
            return new RenderInfo(
                linkTextNode != null ? linkTextNode : label,
                entityConverter.replaceEntities(linkDestination, true, true),
                linkTitle != null ? entityConverter.replaceEntities(linkTitle, true, true) : null
            );
        }
    }

    public static class RsInlineLinkGeneratingProvider extends InlineLinkGeneratingProvider {
        public RsInlineLinkGeneratingProvider(URI baseURI, boolean resolveAnchors) {
            super(baseURI, resolveAnchors);
        }

        @Override
        public void renderLink(@NotNull HtmlGenerator.HtmlGeneratingVisitor visitor, @NotNull String text,
                               @NotNull ASTNode node, @NotNull RenderInfo info) {
            super.renderLink(visitor, text, node, info.copy(info.getLabel(), markLinkAsLanguageItemIfItIsRustPath(info.getDestination()),
                info.getTitle()));
        }
    }
}
