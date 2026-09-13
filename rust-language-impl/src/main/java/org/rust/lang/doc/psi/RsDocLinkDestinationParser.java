/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.doc.psi;

import consulo.language.ast.ASTNode;
import consulo.language.ast.TokenType;
import consulo.language.impl.ast.CompositeElement;
import consulo.language.impl.ast.TreeElement;
import consulo.language.impl.parser.GeneratedParserUtilBase;
import consulo.language.impl.psi.LeafPsiElement;
import consulo.language.parser.ParserDefinition;
import consulo.language.parser.PsiBuilder;
import consulo.language.parser.PsiBuilderFactory;
import consulo.language.util.CharTable;
import consulo.language.version.LanguageVersion;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.lexer.RsLexer;
import org.rust.lang.core.parser.RustParser;

import java.util.Set;

/**
 * Builds the AST of a doc link destination.
 * <p>
 * A destination that looks like a path (possibly decorated with backticks, an anchor and a
 * disambiguator) is re-parsed with the Rust parser, so that intra-doc links like
 * <code>[`Foo::bar`]</code> get a real path inside them and can be resolved. Everything that is not
 * a path is kept as plain text.
 * <p>
 * The decoration around the path is kept as {@code DOC_DATA} leaves, so whatever this class returns
 * covers the whole {@code text} it is given. Otherwise the doc comment AST stops matching the
 * comment token.
 * <p>
 * Mirrors {@code preprocess_link} in {@code src/librustdoc/passes/collect_intra_doc_links.rs}:
 * <ul>
 *     <li>remove backticks</li>
 *     <li>remove the hash suffix</li>
 *     <li>remove the disambiguator prefix/suffix</li>
 *     <li>parse the rest as a path, generics included</li>
 * </ul>
 */
public final class RsDocLinkDestinationParser {

    /** Disambiguators that may precede the path, e.g. {@code fn@foo}. */
    private static final Set<String> KNOWN_PREFIXES = Set.of(
        "struct", "enum", "trait", "union", "module", "mod", "const", "constant", "static",
        "function", "fn", "method", "derive", "type", "value", "macro", "prim", "primitive"
    );

    /** Disambiguators that may follow the path, e.g. {@code foo!()}. */
    private static final String[] KNOWN_SUFFIXES = {"!()", "!{}", "![]", "()", "!"};

    /** Characters that a path may consist of, in addition to letters and digits. */
    private static final String ALLOWED_CHARS = ":_<>, !*&;";

    private RsDocLinkDestinationParser() {
    }

    /**
     * Parses the destination of an inline or a reference link, e.g. {@code bar} in {@code [foo](bar)}.
     */
    @Nonnull
    public static TreeElement parse(@Nonnull CharSequence text, @Nonnull CharTable charTable) {
        TreeElement result = doParse(text, charTable, false);
        return result != null ? result : docDataLeaf(text, charTable);
    }

    /**
     * Parses a short reference link, e.g. {@code [foo]}, as a Rust path.
     *
     * @return {@code null} when the link is not a path, in which case it keeps its Markdown structure
     */
    @Nullable
    public static TreeElement parseShortLink(@Nonnull CharSequence text, @Nonnull CharTable charTable) {
        return doParse(text, charTable, true);
    }

    @Nullable
    private static TreeElement doParse(@Nonnull CharSequence text, @Nonnull CharTable charTable, boolean isShortLink) {
        LinkTextParts info = parseLink(text, isShortLink);
        if (info == null) return null;
        TreeElement path = parseRsPath(info.content());
        if (path == null) return null;
        return createNodes(info, path, charTable);
    }

    /**
     * Strips everything that is not a part of the path and checks that what is left may be one.
     *
     * @return {@code null} if the link certainly is not a path
     */
    @Nullable
    private static LinkTextParts parseLink(@Nonnull CharSequence text, boolean isShortLink) {
        if (text.length() == 0 || indexOf(text, '/', 0) != -1) return null;

        LinkTextParts parts = new LinkTextParts(text);
        if (isShortLink) {
            parts = trimWhitespaces(trimBrackets(parts));
            if (parts == null) return null;
        }
        parts = trimBackticks(parts);
        if (parts == null) return null;
        parts = removeHashAnchor(parts);
        if (parts == null) return null;
        parts = removeDisambiguator(parts);
        if (parts == null) return null;
        return canBeCorrectLink(parts.content()) ? parts : null;
    }

    /** {@code "[func]"} to {@code "func"}. */
    @Nonnull
    private static LinkTextParts trimBrackets(@Nonnull LinkTextParts parts) {
        CharSequence content = parts.content();
        int length = content.length();
        if (length >= 2 && content.charAt(0) == '[' && content.charAt(length - 1) == ']') {
            return parts.subSequence(1, length - 1);
        }
        return parts;
    }

    /**
     * {@code "`func`"} to {@code "func"}.
     * <p>
     * Removes any number of backticks at the beginning and at the end. Rustdoc also removes backticks
     * in the middle, but that is not supported here.
     */
    @Nullable
    private static LinkTextParts trimBackticks(@Nonnull LinkTextParts parts) {
        return trimChar(parts, c -> c == '`');
    }

    @Nullable
    private static LinkTextParts trimWhitespaces(@Nullable LinkTextParts parts) {
        return parts == null ? null : trimChar(parts, Character::isWhitespace);
    }

    /**
     * @return {@code null} if the whole content matches the filter, i.e. nothing is left
     */
    @Nullable
    private static LinkTextParts trimChar(@Nonnull LinkTextParts parts, @Nonnull CharFilter filter) {
        CharSequence content = parts.content();
        int start = 0;
        int end = content.length();
        // the bound also covers content that is entirely made of trimmed characters, e.g. "```"
        while (start < end && filter.accept(content.charAt(start))) {
            ++start;
        }
        if (start == end) return null;
        while (filter.accept(content.charAt(end - 1))) {
            --end;
        }
        return parts.subSequence(start, end);
    }

    /**
     * {@code "mod1::mod2#anchor"} to {@code "mod1::mod2"}.
     *
     * @return {@code null} for a link with several {@code #}'s or with an empty path
     */
    @Nullable
    private static LinkTextParts removeHashAnchor(@Nonnull LinkTextParts parts) {
        CharSequence content = parts.content();
        int hashIndex = indexOf(content, '#', 0);
        if (hashIndex == -1) return parts;  // no anchors
        if (indexOf(content, '#', hashIndex + 1) != -1) return null;  // multiple #'s - invalid link

        // anchor to an element of the current page - ignore
        if (isBlank(content, 0, hashIndex)) return null;

        int anchorLength = content.length() - hashIndex;
        return parts.removeSuffix(anchorLength);
    }

    /**
     * {@code "fn@func"} to {@code "func"}, {@code "gen!()"} to {@code "gen"}.
     *
     * @return {@code null} for an unknown disambiguator or an empty path
     */
    @Nullable
    private static LinkTextParts removeDisambiguator(@Nonnull LinkTextParts parts) {
        CharSequence content = parts.content();
        int index = indexOf(content, '@', 0);
        if (index != -1) {
            String prefix = content.subSequence(0, index).toString();
            if (!KNOWN_PREFIXES.contains(prefix)) return null;
            LinkTextParts result = parts.removePrefix(prefix.length() + "@".length());
            CharSequence rest = result.content();
            return isBlank(rest, 0, rest.length()) ? null : result;
        }
        for (String suffix : KNOWN_SUFFIXES) {
            if (content.length() > suffix.length() && endsWith(content, suffix)) {
                return parts.removeSuffix(suffix.length());
            }
        }
        return parts;
    }

    private static boolean canBeCorrectLink(@Nonnull CharSequence link) {
        for (int i = 0; i < link.length(); i++) {
            char c = link.charAt(i);
            if (!isAlphanumeric(c) && ALLOWED_CHARS.indexOf(c) == -1) return false;
        }
        return true;
    }

    private static boolean isAlphanumeric(char c) {
        return Character.isAlphabetic(c) || Character.isDigit(c);
    }

    /**
     * Wraps the parsed path together with the leaves for the stripped prefix and suffix.
     *
     * @return the first node of the chain; the following nodes are reachable as its siblings
     */
    @Nullable
    private static TreeElement createNodes(@Nonnull LinkTextParts info,
                                           @Nonnull TreeElement path,
                                           @Nonnull CharTable charTable) {
        CompositeElement root = RsDocElementTypes.DOC_LINK_DEFINITION.createCompositeNode();
        CharSequence prefix = info.prefix();
        CharSequence suffix = info.suffix();
        if (prefix.length() != 0) {
            root.rawAddChildrenWithoutNotifications(docDataLeaf(prefix, charTable));
        }
        root.rawAddChildrenWithoutNotifications(path);
        if (suffix.length() != 0) {
            root.rawAddChildrenWithoutNotifications(docDataLeaf(suffix, charTable));
        }
        return root.getFirstChildNode();
    }

    /**
     * Parses the text with the Rust parser as a path with optional generic arguments.
     *
     * @return {@code null} if the text is not a well-formed path
     */
    @Nullable
    private static TreeElement parseRsPath(@Nonnull CharSequence pathText) {
        ParserDefinition parserDefinition = ParserDefinition.forLanguage(RsLanguage.INSTANCE);
        if (parserDefinition == null) {
            throw new IllegalStateException("No parser definition for language " + RsLanguage.INSTANCE);
        }
        LanguageVersion languageVersion = RsLanguage.INSTANCE.getVersions()[0];
        PsiBuilder plainBuilder = PsiBuilderFactory.getInstance()
            // The builder interns the path leaves in a char table of its own: the PsiBuilderImpl
            // constructor taking one is not reachable from a plugin, consulo.language.impl.internal.parser
            // being exported only to consulo.ide.impl and consulo.test.impl. Interning is a memory
            // optimisation, so only string sharing with the rest of the comment tree is lost.
            .createBuilder(parserDefinition, new RsLexer(), languageVersion, pathText);

        PsiBuilder builder = GeneratedParserUtilBase.adapt_builder_(
            RsDocElementTypes.DOC_LINK_DEFINITION,
            plainBuilder,
            new RustParser(),
            RustParser.EXTENDS_SETS_
        );

        PsiBuilder.Marker rootMarker = GeneratedParserUtilBase.enter_section_(builder, 0, GeneratedParserUtilBase._NONE_, null);

        if (!RustParser.TypePathGenericArgs(builder, 0)) {
            return null;
        }

        GeneratedParserUtilBase.exit_section_(
            builder,
            0,
            rootMarker,
            RsDocElementTypes.DOC_LINK_DEFINITION,
            true,
            true,
            GeneratedParserUtilBase.TRUE_CONDITION
        );

        ASTNode treeBuilt = builder.getTreeBuilt();
        // the text that the parser did not consume ends up in an error element
        if (treeBuilt.findChildByType(TokenType.ERROR_ELEMENT) != null) return null;
        return (TreeElement)treeBuilt.getFirstChildNode();
    }

    @Nonnull
    private static TreeElement docDataLeaf(@Nonnull CharSequence text, @Nonnull CharTable charTable) {
        return new LeafPsiElement(RsDocElementTypes.DOC_DATA, charTable.intern(text));
    }

    private static int indexOf(@Nonnull CharSequence text, char c, int fromIndex) {
        for (int i = fromIndex; i < text.length(); i++) {
            if (text.charAt(i) == c) return i;
        }
        return -1;
    }

    private static boolean endsWith(@Nonnull CharSequence text, @Nonnull String suffix) {
        int offset = text.length() - suffix.length();
        if (offset < 0) return false;
        for (int i = 0; i < suffix.length(); i++) {
            if (text.charAt(offset + i) != suffix.charAt(i)) return false;
        }
        return true;
    }

    private static boolean isBlank(@Nonnull CharSequence text, int start, int end) {
        for (int i = start; i < end; i++) {
            if (!Character.isWhitespace(text.charAt(i))) return false;
        }
        return true;
    }

    @FunctionalInterface
    private interface CharFilter {
        boolean accept(char c);
    }

    /**
     * A view of the link text split into a prefix, the content and a suffix:
     * <pre>
     * fn@mod1::mod2::func#anchor
     * ~~~ prefix         ~~~~~~~ suffix
     *    ~~~~~~~~~~~~~~~~ content
     * </pre>
     * The prefix and the suffix are what has already been stripped away; the content is what is
     * going to be parsed as a path.
     */
    private static final class LinkTextParts {
        private final CharSequence text;
        private final int startOffset;
        private final int endOffset;

        LinkTextParts(@Nonnull CharSequence text) {
            this(text, 0, text.length());
        }

        private LinkTextParts(@Nonnull CharSequence text, int startOffset, int endOffset) {
            this.text = text;
            this.startOffset = startOffset;
            this.endOffset = endOffset;
        }

        @Nonnull
        CharSequence prefix() {
            return text.subSequence(0, startOffset);
        }

        @Nonnull
        CharSequence suffix() {
            return text.subSequence(endOffset, text.length());
        }

        @Nonnull
        CharSequence content() {
            return text.subSequence(startOffset, endOffset);
        }

        private int contentLength() {
            return endOffset - startOffset;
        }

        @Nonnull
        LinkTextParts subSequence(int start, int end) {
            if (start == 0 && end == contentLength()) return this;
            if (start < 0 || start > end || end > contentLength()) {
                throw new IllegalArgumentException("Invalid range [" + start + ", " + end + ") in " + text);
            }
            return new LinkTextParts(text, startOffset + start, startOffset + end);
        }

        @Nonnull
        LinkTextParts removePrefix(int length) {
            return subSequence(length, contentLength());
        }

        @Nonnull
        LinkTextParts removeSuffix(int length) {
            return subSequence(0, contentLength() - length);
        }
    }
}
